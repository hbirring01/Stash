package com.example.creditcardapp.data.ai

/**
 * Curated presets for OpenAI-compatible LLM providers.
 *
 * All four providers expose the same `POST /chat/completions` wire format, so
 * we ship one Retrofit-less HTTP client and just swap `baseUrl` + `model` +
 * `apiKey`. Users can also pick [CUSTOM] to point at Ollama, vLLM, or any other
 * OpenAI-compatible gateway they self-host.
 *
 * Defaults are chosen so the free tier is the path of least resistance:
 *  - GEMINI: 1,500 req/day Flash free tier, one-click key at aistudio.google.com
 *  - GROQ:   ~14k req/day Llama-8B free tier, blazing fast (~500 tok/s)
 *  - OPENROUTER: aggregator with `:free` model variants and one API key
 */
enum class AiProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val signupHint: String,
) {
    GEMINI(
        displayName = "Google Gemini (free)",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
        defaultModel = "gemini-2.0-flash",
        signupHint = "Get a free API key at aistudio.google.com → Get API key",
    ),
    GROQ(
        displayName = "Groq (free)",
        defaultBaseUrl = "https://api.groq.com/openai/v1/",
        defaultModel = "llama-3.1-8b-instant",
        signupHint = "Get a free API key at console.groq.com/keys",
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1/",
        defaultModel = "meta-llama/llama-3.1-8b-instruct:free",
        signupHint = "Get a free API key at openrouter.ai/keys",
    ),
    CUSTOM(
        displayName = "Custom (Ollama, self-hosted)",
        defaultBaseUrl = "",
        defaultModel = "",
        signupHint = "Point base URL at any OpenAI-compatible endpoint",
    ),
}
