@echo off
echo Script para compilar localmente no Windows com Android SDK CLI
set BUILD_TOOLS=%ANDROID_HOME%\build-tools\34.0.0
set PLATFORM=%ANDROID_HOME%\platforms\android-34\android.jar

mkdir build\compiled_res build\gen build\classes build\dex dist 2>nul

"%BUILD_TOOLS%\aapt2.exe" compile --dir res -o build\compiled_res\resources.zip
"%BUILD_TOOLS%\aapt2.exe" link -o build\unsigned.unaligned.apk -I "%PLATFORM%" --manifest AndroidManifest.xml --java build\gen build\compiled_res\resources.zip
javac -source 1.8 -target 1.8 -bootclasspath "%PLATFORM%" -classpath "%PLATFORM%" -d build\classes src\com\opensource\volumebooster\*.java build\gen\com\opensource\volumebooster\R.java
"%BUILD_TOOLS%\d8.bat" --lib "%PLATFORM%" --output build\dex build\classes\com\opensource\volumebooster\*.class
powershell -Command "Add-Type -Assembly 'System.IO.Compression.FileSystem'; $zip = [System.IO.Compression.ZipFile]::Open('build\unsigned.unaligned.apk', 'Update'); [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, 'build\dex\classes.dex', 'classes.dex'); $zip.Dispose()"
"%BUILD_TOOLS%\zipalign.exe" -p -f -v 4 build\unsigned.unaligned.apk build\unsigned.aligned.apk
echo Pronto! Assine com apksigner.
