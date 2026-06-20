# SoundSpot + YouTube Music ഇന്റഗ്രേഷൻ — README

## എന്താണ് ചെയ്തത്

ഈ പ്രോജക്റ്റ് (Compose music app) ലെ വ്യാജ ബാക്കെൻഡ് (SoundSpot/Elasticsearch placeholder) മാറ്റി,
**NewPipeExtractor** ലൈബ്രറി ഉപയോഗിച്ച് യഥാർത്ഥ YouTube സെർച്ച് + ഓഡിയോ സ്ട്രീമിംഗ് ലോജിക്
("Eco YouTube Music" extension-ൽ നിന്ന് adapt ചെയ്തത്) നേരിട്ട് വയർ ചെയ്തു.

**മാറ്റിയ ഫീച്ചറുകൾ:** Search → Play → Favorites (നിങ്ങൾ പറഞ്ഞ scope പ്രകാരം).
**UI:** ഏതാണ്ട് മാറ്റിയിട്ടില്ല. "Elasticsearch" എന്ന് പറയുന്ന ടെക്സ്റ്റ് ലേബലുകൾ മാത്രം
"YouTube Music" എന്നാക്കി മാറ്റി (compile-ന് ആവശ്യമില്ലെങ്കിലും confusion ഒഴിവാക്കാൻ).
യൂട്യൂബ് thumbnail കാണിക്കാൻ Coil ഉം ചേർത്തു (ഇല്ലെങ്കിൽ പഴയ procedural art തന്നെ കാണിക്കും).

### പ്രധാന മാറ്റങ്ങൾ ഉള്ള ഫയലുകൾ
- `app/src/main/java/com/example/data/youtube/YouTubeMusicService.kt` **(പുതിയത്)** — NewPipeExtractor വഴി
  search + audio stream URL resolve.
- `data/model/Track.kt` — `youtubeVideoId`, `thumbnailUrl` ഫീൽഡുകൾ ചേർത്തു.
- `data/repository/MusicRepository.kt` — `searchYouTube()`, `resolvePlayableUrl()`,
  `toggleFavoriteTrack()` (upsert) ചേർത്തു.
- `ui/MusicViewModel.kt` — സെർച്ച്, പ്ലേ, ഫേവറിറ്റ് ലോജിക് പുതിയ functions-ലേക്ക് wire ചെയ്തു.
- `ui/components/VibeAlbumArt.kt` + 4 screen ഫയലുകൾ — thumbnail support.
- `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` — NewPipeExtractor,
  Coil, desugaring dependencies + JitPack repo ചേർത്തു.

## Build ചെയ്യേണ്ടത് എങ്ങനെ

1. Android Studio (ഏറ്റവും പുതിയ stable version) തുറക്കുക → **Open** → ഈ folder select ചെയ്യുക.
2. "Sync Now" ഞെക്കുക (ഇതിന് internet വേണം — NewPipeExtractor, Coil dependencies download ചെയ്യും).
3. `.env` ഫയൽ ഉണ്ടാക്കണം (Gemini "AI DJ" ഫീച്ചറിന് വേണ്ടി മാത്രം, വേണമെങ്കിൽ ഒഴിവാക്കാം):
   `GEMINI_API_KEY=your_key_here`
4. `app/build.gradle.kts`-ൽ `signingConfig = signingConfigs.getByName("debugConfig")` എന്ന ലൈൻ
   ഒറിജിനൽ README അനുസരിച്ച് വേണമെങ്കിൽ മാറ്റുക (debug run-ന് വേണ്ടി).
5. **Run ▶** അല്ലെങ്കിൽ **Build → Build Bundle(s)/APK(s) → Build APK(s)**.

## അറിയേണ്ട പ്രധാന കാര്യങ്ങൾ

- **ഞാൻ ഇത് ഇവിടെ compile ചെയ്ത് test ചെയ്തിട്ടില്ല** — ഈ sandbox-ൽ internet ഇല്ലാത്തതിനാൽ Gradle
  dependencies download ചെയ്യാൻ പറ്റില്ല. NewPipeExtractor API യുടെ logic, "Eco YouTube Music"-ലെ
  സമാന, പ്രവർത്തിക്കുന്ന കോഡിൽ നിന്നുതന്നെ പകർത്തിയതാണ് — പക്ഷെ Android Studio-യിൽ build ചെയ്യുമ്പോൾ
  1-2 ചെറിയ compile error വന്നേക്കാം (e.g. NewPipeExtractor version-specific API പേരുകൾ). അത്
  സംഭവിച്ചാൽ error message എനിക്ക് അയച്ചാൽ ശരിയാക്കാം.
- YouTube stream URL-കൾ കുറച്ച് മണിക്കൂറുകൾ കഴിഞ്ഞാൽ expire ആകും — അതുകൊണ്ട് ഓരോ play-യിലും
  fresh URL resolve ചെയ്യുന്നു (network വേണം, ചെറിയ delay ഉണ്ടാകാം).
- ഇത് YouTube-ന്റെ ഔദ്യോഗിക API അല്ല, ad-skip ചെയ്ത് audio നേരിട്ട് extract ചെയ്യുന്നതാണ് —
  ഇത് YouTube ToS പ്രകാരം നിയന്ത്രണങ്ങൾ ഉള്ള ഒരു gray area ആണ് (NewPipe പോലുള്ള ഓപ്പൺ സോഴ്സ്
  ആപ്പുകളിൽ ഇതേ technique ഉപയോഗിക്കുന്നു). സ്വന്തം ഉപയോഗത്തിന് മാത്രം വയ്ക്കുക, Play Store-ൽ
  publish ചെയ്യാതിരിക്കുക.
