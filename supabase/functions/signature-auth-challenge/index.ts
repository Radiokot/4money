// Returns a short-living challenge to be signed in order to be authorized.
// Doesn't require a token to be called.

import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { signJWT } from "jsr:@cross/jwt"

Deno.serve(async (req) => {

  const payload = {
    n: "4Money auth " + crypto.randomUUID(),
  }

  // This is just a long random string, not a PEM-encoded key.
  const secret = Deno.env.get("SIGNATURE_AUTH_CHALLENGE_SECRET")
  if (!secret) {
    throw Error("Missing auth challenge secret")
  }

  const jwtOptions = {
    algorithm: "HS256",
    expiresIn: "1m",
  }

  const challenge = await signJWT(
    payload,
    secret,
    jwtOptions,
  )

  return new Response(
    challenge,
    { headers: { "Content-Type": "text/plain" } },
  )
})
