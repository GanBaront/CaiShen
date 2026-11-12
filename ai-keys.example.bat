@echo off
rem =====================================================================
rem  AI KEYS TEMPLATE
rem  1) copy this file to ai-keys.bat
rem  2) fill in your own keys
rem  3) run start-all.bat - it injects ai-keys.bat automatically
rem
rem  ai-keys.bat holds real secrets: never commit it, never ship it
rem  inside a source zip you send to other people.
rem =====================================================================

rem ---- Text model: recipe ranking / AI recipe creation ----
rem DeepSeek: https://platform.deepseek.com  (OpenAI compatible)
rem Other compatible providers (Zhipu / DashScope / Moonshot / OpenAI) just
rem need base-url + model + key swapped.
set "AI_BASE_URL=https://api.deepseek.com/v1"
set "AI_MODEL=deepseek-chat"
set "AI_API_KEY="

rem ---- Vision model: dish / ingredient photo recognition ----
rem Zhipu GLM-4V: https://open.bigmodel.cn  (glm-4v-flash is free)
rem Leave AI_VISION_BASE_URL / AI_VISION_API_KEY empty to reuse the text ones.
set "AI_VISION_BASE_URL=https://open.bigmodel.cn/api/paas/v4"
set "AI_VISION_MODEL=glm-4v-flash"
set "AI_VISION_API_KEY="

rem Optional: image payload format for the vision model
rem   auto (default) | data-uri | raw-base64
rem set "AI_VISION_IMAGE_MODE=auto"
