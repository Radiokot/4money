// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from "jsr:@supabase/supabase-js@2"

class UsdPriceEntry {
  code: string;
  price: number;
  source: string;

  constructor(code: string, price: number, source: string) {
    this.code = code;
    this.price = price
    this.source = source
  }

  static fromBinanceUsdtTickerPrice(tickerPrice: any): UsdPriceEntry {
    return new UsdPriceEntry(
      tickerPrice.symbol.replace("USDT", ""),
      parseFloat(tickerPrice.price),
      "binance",
    )
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

const BINANCE_API = "https://api.binance.com/api/v3/ticker/price"
const WISE_API = "https://api.wise.com/v1/rates?target=USD"
const WISE_API_KEY = Deno.env.get("WISE_API_KEY") ?? ""
const KUCOIN_API = "https://api.kucoin.com/api/v1/market/allTickers"
const CRYPTO_WHITELIST = new Set<string>([
  "BTC", "ETH", "XMR", "USDT", "CCD",
])

async function fetchBinancePrices(): Promise<UsdPriceEntry[]> {
  try {
    const response = await fetch(BINANCE_API)
    if (!response.ok) {
      throw new Error(`Binance API error: ${response.status}`)
    }

    const data = await response.json()

    const entries = data
      .filter(item => item.symbol.endsWith("USDT"))
      .map(item => UsdPriceEntry.fromBinanceUsdtTickerPrice(item))
      .filter(usdPriceEntry => CRYPTO_WHITELIST.has(usdPriceEntry.code))

    return entries
  } catch (error) {
    console.error("Error fetching from Binance:", error)
    return []
  }
}

async function fetchWisePrices(): Promise<UsdPriceEntry[]> {
  try {
    const response = await fetch(WISE_API, {
      headers: {
        "Authorization": `Bearer ${WISE_API_KEY}`
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
  try {
    const [
      binancePrices,
      wisePrices,
      kucoinPrices,
    ] = await Promise.all([
      fetchBinancePrices(),
      fetchWisePrices(),
      fetchKucoinPrices(),
    ])
    const averagePrices = mergePrices(
      binancePrices,
      wisePrices,
      kucoinPrices,
      [UsdPriceEntry.USDT],
    )

    const serviceClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
    )

    // Insert/update pairs.
    const { error } = await serviceClient
      .from("pairs")
      .upsert(
        averagePrices
          .map(usdPriceEntry => ({
            base_currency_code: usdPriceEntry.code,
            quote_currency_code: "USD",
            price: usdPriceEntry.price,
          })),
        {
          ignoreDuplicates: false,
          returning: "minimal",
        }
      )

    if (error) {
      throw error
    }

    return new Response(
      JSON.stringify({ done: true }),
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
