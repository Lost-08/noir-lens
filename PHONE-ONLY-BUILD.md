# Build Noir Lens using only your Android phone

This project includes `.github/workflows/build-apk.yml`, which builds a debug APK on GitHub's cloud runners. You do not need a computer.

1. Download and extract `NoirLens-source.zip` on your phone. If your file manager cannot extract it, install a trusted archive manager.
2. In your phone browser, sign in to GitHub and create a new repository named `noir-lens`.
3. Upload the *contents* of the `NoirLens` folder (including the hidden `.github` folder and its workflow file) to the repository. If GitHub's mobile upload page does not show hidden files or folders, use GitHub's web interface in Desktop site mode, or create the workflow file manually at `.github/workflows/build-apk.yml` using the exact file included in this project.
4. Open the repository's **Actions** tab. Select **Build Noir Lens APK** and tap **Run workflow**. If Actions is disabled, enable it in the repository's Actions settings.
5. Wait for the workflow to finish. A green check means the cloud build succeeded; a red X means the build failed and its logs need to be checked.
6. Open the completed workflow run, scroll to **Artifacts**, and download `noir-lens-debug-apk`.
7. Extract the downloaded artifact ZIP to find `app-debug.apk`, then tap it to install. Android may ask you to allow installation from your browser or file manager. Only install APKs you built or trust.

Notes:
- The APK is a debug build for testing, not a signed release for Play Store publishing.
- The first cloud build may fail if the project has a source or dependency issue; review the Actions logs and fix the reported issue before trying again.
- No signing keys or secrets are needed for a debug APK.
