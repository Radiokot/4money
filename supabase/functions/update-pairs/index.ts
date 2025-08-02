// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import postgres from 'https://deno.land/x/postgresjs/mod.js'

class UsdPriceEntry {
  code: string;
  price: number;
  source: string;

  constructor(code: string, price: number, source: string) {
    this.code = code;
    this.price = price
    this.source = source
  }

  static fromWiseUsdRate(usdRate: any): UsdPriceEntry {
    return new UsdPriceEntry(
      usdRate.source,
      parseFloat(usdRate.rate),
      "wise"
    )
  }

  static fromKucoinUsdtTickerPrice(tickerPrice: any): UsdPriceEntry {
    return new UsdPriceEntry(
      tickerPrice.symbol.replace("-USDT", ""),
      parseFloat(tickerPrice.buy),
      "kucoin",
    )
  }

  static fromMultiplePrices(code: string, prices: number[]): UsdPriceEntry {
    const sum = prices.reduce((acc: number, val: number) => acc + val, 0)
    const average = prices.length > 0 ? sum / prices.length : 0
    return new UsdPriceEntry(
      code,
      average,
      "average",
    )
  }

  static USDT = new UsdPriceEntry("USDT", 1, "stablecoin")
}

const KUCOIN_API = "https://api.kucoin.com/api/v1/market/allTickers"
const CRYPTO_WHITELIST = new Set<string>([
  "BTC", "ETH", "XMR", "CCD",
])

const sql = postgres(Deno.env.get("SUPABASE_DB_URL"))

async function fetchBinancePrices(time: Date): Promise<UsdPriceEntry[]> {
  try {
    const tickerData: any[] = await fetch("https://api.binance.com/api/v3/ticker/price")
      .then(response => {
        if (!response.ok) {
          throw new Error(`Binance ticker API error: ${response.status}`)
        }
        return response.json()
      })

    const listedCurrencies = tickerData
      .filter(item => item.symbol.endsWith("USDT"))
      .map(item => item.symbol.substring(0, item.symbol.lastIndexOf("USDT")))
      .filter(code => CRYPTO_WHITELIST.has(code))

    const entries: (UsdPriceEntry | null)[] = await Promise.all(
      listedCurrencies.map(code => {
        
        const dayStartDate = new Date(time)
        dayStartDate.setUTCHours(0, 0, 0, 0)
        const dayStartMillis = dayStartDate.getTime()
        const dayEndMillis = dayStartMillis + 24 * 3600000 - 1

        const klinesUrl = new URL("https://api.binance.com/api/v3/klines")
        klinesUrl.searchParams.set("symbol", code + "USDT")
        klinesUrl.searchParams.set("interval", "1d")
        klinesUrl.searchParams.set("startTime", dayStartMillis.toString())
        klinesUrl.searchParams.set("endTime", dayEndMillis.toString())

        return fetch(klinesUrl)
          .then(response => {
            if (!response.ok) {
              throw new Error(`Binance klines API error: ${response.status}`)
            }
            return response.json()
          })
          .then(klinesData => klinesData as any[])
          .then(klinesData => {
            if (klinesData.length == 0) {
              return null
            }

            // Closing price.
            return new UsdPriceEntry(code, +klinesData[0][4], "binance-klines")
          })
      })
    )

    return entries.filter(entry => entry != null) as UsdPriceEntry[]
  } catch (error) {
    console.error("Error fetching historical prices from Binance:", error)
    return []
  }
}

async function fetchWisePrices(time: Date): Promise<UsdPriceEntry[]> {
  try {
    const url = new URL("https://api.wise.com/v1/rates")
    url.searchParams.set("time", time.toISOString())
    url.searchParams.set("target", "USD")

    const response = await fetch(url, {
      headers: {
        "Authorization": `Bearer ${Deno.env.get("WISE_API_KEY") ?? ""}`
      }
    })
    if (!response.ok) {
      throw new Error(`Wise API error: ${response.status}`)
    }

    const data = await response.json()

    return data
      .map(item => UsdPriceEntry.fromWiseUsdRate(item))
  } catch (error) {
    console.error("Error fetching from Wise:", error)
    return []
  }
}

async function fetchKucoinPrices(): Promise<UsdPriceEntry[]> {
  try {
    const response = await fetch(KUCOIN_API)
    if (!response.ok) {
      throw new Error(`Kucoin API error: ${response.status}`)
    }

    const data = (await response.json()).data

    return data
      .ticker
      .filter(item => item.symbol.endsWith("-USDT"))
      .map(item => UsdPriceEntry.fromKucoinUsdtTickerPrice(item))
      .filter(usdPriceEntry => CRYPTO_WHITELIST.has(usdPriceEntry.code))
  } catch (error) {
    console.error("Error fetching from Kucoin:", error)
    return []
  }
}

function mergePrices(...prices: UsdPriceEntry[][]): UsdPriceEntry[] {
  const pricesByCode: Map<string, number[]> = new Map()

  prices.forEach(sourcePrices => {
    sourcePrices.forEach(priceEntry => {
      if (pricesByCode.has(priceEntry.code)) {
        pricesByCode.get(priceEntry.code)!.push(priceEntry.price)
      } else {
        pricesByCode.set(priceEntry.code, [priceEntry.price])
      }
    })
  })

  return Array.from(pricesByCode.entries())
    .map(([code, prices]) => UsdPriceEntry.fromMultiplePrices(code, prices))
}

Deno.serve(async (req) => {

  const timeString: string | null = new URL(req.url).searchParams?.get("time")
  let time: Date = new Date()
  if (timeString) {
    time = new Date(Date.parse(timeString))
  }

  try {
    const [
      binancePrices,
      wisePrices,
      kucoinPrices,
    ] = await Promise.all([
      fetchBinancePrices(time),
      fetchWisePrices(time),
      fetchKucoinPrices(),
    ])
    const averagePrices = mergePrices(
      binancePrices,
      wisePrices,
      kucoinPrices,
      [UsdPriceEntry.USDT],
    )

    if (!timeString) {
      const pairValues = averagePrices.map(usdPriceEntry => ({
        base_currency_code: usdPriceEntry.code,
        quote_currency_code: "USD",
        price: usdPriceEntry.price,
      }))

      await sql`
        INSERT INTO pairs ${sql(pairValues)}
        ON CONFLICT (base_currency_code, quote_currency_code) 
        DO UPDATE SET price = EXCLUDED.price;
      `
    }

    const dailyPriceValues = averagePrices.map(usdPriceEntry => ({
      base_currency_code: usdPriceEntry.code,
      day: time.toISOString().substring(0, 10),
      samples: 1,
      price: usdPriceEntry.price,
    }))

    await sql`
      INSERT INTO daily_prices ${sql(dailyPriceValues)}
      ON CONFLICT (base_currency_code, day)
      DO UPDATE SET 
        price = trim_scale(daily_prices.price + (EXCLUDED.price - daily_prices.price) / (daily_prices.samples + 1)),
        samples = daily_prices.samples + 1
    `
    return new Response(
      JSON.stringify({
        done: true,
        time: time,
      }),
      {
        headers: { "Content-Type": "application/json" }
      }
    )
  } catch (error) {
    console.error("Error processing request:", error)
    return new Response(
      JSON.stringify({ error: "Failed to update pairs" }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    )
  }
})
