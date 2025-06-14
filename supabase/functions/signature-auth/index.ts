// Returns a session for the user associated the private key
// used to sign the auth challenge.
// Doesn't require a token to be called.

import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { validateJWT } from "jsr:@cross/jwt"
import * as ed25519 from "jsr:@noble/ed25519"
import { createClient } from "jsr:@supabase/supabase-js@2"
import * as uuid from "jsr:@std/uuid"

Deno.serve(async (req) => {

  const {
    challenge,
    signatureHex,
    pubkeyHex,
  } = await req.json()

  // This is just a long random string, not a PEM-encoded key.
  const secret = Deno.env.get("SIGNATURE_AUTH_CHALLENGE_SECRET")
  if (!secret) {
    throw Error("Missing auth challenge secret")
  }

  // Check if the challenge (signature-auth-challenge) is correct and not outdated.
  try {
    await validateJWT(
      challenge,
      secret,
      {
        validateExp: true,
      }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: "Challenge verification failed", details: error.message }),
      {
        status: 400,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  let pubkey: ed25519.Point
  try {
    pubkey = ed25519.Point.fromHex(pubkeyHex)
  } catch (error) {
    return new Response(
      JSON.stringify({ error: "Public key decoding failed", details: error.message }),
      {
        status: 400,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  // Check if the challenge is signed a private key
  // which corresponds to the specified public key.
  try {
    const signedBytes = new TextEncoder().encode(challenge)
    await ed25519.verifyAsync(signatureHex, signedBytes, pubkeyHex)
  } catch (error) {
    return new Response(
      JSON.stringify({ error: "Signature verification failed", details: error.message }),
      {
        status: 400,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  // Derive identifiers from the public key,
  // so they're always the same for the given key.
  const userIdentifier = "4Money-User-" + pubkeyHex
  const userUuid = await uuid.v5.generate(uuid.NAMESPACE_DNS, userIdentifier + ".4mn.local")
  const userEmail = userUuid + "@4mn.local"

  // As it is not possible to create a session without credentials,
  // use a temp random password to sign in.
  const userTempPassword = crypto.randomUUID()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
    {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    }
  )

  const { error: userCreationError } = await supabase.auth.admin.createUser({
    id: userUuid,
    email: userEmail,
    email_confirm: true,
  })

  if (userCreationError && userCreationError.code != "email_exists") {
    console.error("User creation failed", userCreationError)
    return new Response(
      JSON.stringify({ error: "User creation failed", details: userCreationError.code }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  const { error: passwordUpdateError } = await supabase.auth.admin.updateUserById(
    userUuid,
    {
      password: userTempPassword,
    }
  )

  if (passwordUpdateError) {
    console.error("User password update failed", passwordUpdateError)
    return new Response(
      JSON.stringify({ error: "User password update failed", details: passwordUpdateError.code }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  const { data: signInData, error: signInError } = await supabase.auth.signInWithPassword({
    email: userEmail,
    password: userTempPassword,
  })

  if (signInError) {
    console.error("Sign in failed", signInError)
    return new Response(
      JSON.stringify({ error: "Sign in failed", details: signInError.code }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  return new Response(
    JSON.stringify(signInData.session),
    { headers: { "Content-Type": "text/plain" } },
  )
})
