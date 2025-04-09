# ✨ LLM Android App Showcase ✨

**A comparative collection of Android applications generated by LLMs, showcasing their capabilities in mobile app development.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) 
[![GitHub issues](https://img.shields.io/github/issues/sanadlab/vibe_coded_android_apps)](https://github.com/your-username/your-repo-name/issues)
[![GitHub forks](https://img.shields.io/github/forks/sanadlab/vibe_coded_android_apps)](https://github.com/your-username/your-repo-name/network)
[![GitHub stars](https://img.shields.io/github/stars/sanadlab/vibe_coded_android_apps)](https://github.com/sanadlab/vibe_coded_android_apps/stargazers)

---

## 🤔 What is This?

This repository serves as an experimental playground comparing Android applications generated entirely by different state-of-the-art Large Language Models (LLMs). We define common application concepts (like a weather app, a game, a notes app) and prompt different LLMs to generate the full Android project code.

The goals are to:

1.  **Explore LLM Capabilities:** Assess how well different models can generate functional, structured Android applications (using Kotlin/Java, XML layouts, Gradle builds).
2.  **Evaluate non-functional requirements on LLM generated code** Observe differences in code structure, performance, adherence to Android best practices, etc.

## 📱 The Concept: Standard Android Apps

This repository focuses on generating more conventional Android applications with defined functionalities, such as:

*   Utility Apps (Weather, Calculator, To-Do Lists)
*   Simple Games (2048, Flappy Bird)
*   Content Display Apps (Photo Gallery)

## 📂 Repository Structure

The repository is organized by the LLM that generated the code, with each specific application contained within its own standard Android project directory.



**Note:**
*   Each application directory (e.g., `WeatherApp`, `GPTGame2048`) is a self-contained Android Studio project.
*   Some projects use Groovy (`.gradle`) for build scripts, while others use Kotlin Script (`.gradle.kts`). This is one point of comparison.
*   Common files like `.idea` (IDE settings) and `.gradle` (Gradle cache) are typically excluded via `.gitignore`.

## 🤖 LLMs Featured

This repository currently includes code generated by:

*   **✨ Google Gemini Pro 2.5:** [Link to Google AI Studio or Vertex AI]
*   **🤖 OpenAI GPT-4o:** [Link to OpenAI Platform]

<!-- NOTE: Replace bracketed links with actual URLs if desired -->

## 💡 Android Apps Included

**✅ Validation Note:** Each application listed below has been successfully built using Android Studio and run on an emulator or physical device to validate its basic functionality at the time of commit. While core features were confirmed to work, this does not guarantee bug-free operation or adherence to all best practices. Please refer to the Disclaimer section for more details on the experimental nature of this code.

Here are the applications generated, allowing for direct comparison between the LLMs:

1.  **Weather App**
    *   **Description:** Displays current weather conditions and forecasts, likely requiring location permissions and a weather API key.
    *   **Implementations:**
        *   Gemini Pro 2.5: [`gemini_pro_2.5/WeatherApp/`](./gemini_pro_2.5/WeatherApp/)
        *   GPT-4o: [`gpt4-o/GPTWeather/`](./gpt4-o/GPTWeather/)

2.  **2048 Game**
    *   **Description:** A clone of the popular sliding tile puzzle game "2048".
    *   **Implementations:**
        *   Gemini Pro 2.5: [`gemini_pro_2.5/game2048/`](./gemini_pro_2.5/game2048/)
        *   GPT-4o: [`gpt4-o/GPTGame2048/`](./gpt4-o/GPTGame2048/)

3.  **To-Do Notes App**
    *   **Description:** Allows users to create, view, edit notes written in Markdown and text format.
    *   **Implementations:**
        *   Gemini Pro 2.5: [`gemini_pro_2.5/ToDoNotes/`](./gemini_pro_2.5/ToDoNotes/)
        *   GPT-4o: [`gpt4-o/GPTToDoNotes/`](./gpt4-o/GPTToDoNotes/)

4.  **Photo Gallery App**
    *   **Description:** Displays images stored on the user's device, requiring storage permissions.
    *   **Implementations:**
        *   Gemini Pro 2.5: [`gemini_pro_2.5/GalleryApp/`](./gemini_pro_2.5/GalleryApp/)
        *   GPT-4o: [`gpt4-o/GPTPhotoGallery/`](./gpt4-o/GPTPhotoGallery/)

5.  **Scientific Calculator**
    *   **Description:** A calculator application with standard arithmetic operations and scientific functions.
    *   **Implementations:**
        *   Gemini Pro 2.5: [`gemini_pro_2.5/ScientificCalculator/`](./gemini_pro_2.5/ScientificCalculator/)
        *   GPT-4o: [`gpt4-o/GPTScientificCalculator/`](./gpt4-o/GPTScientificCalculator/)

6.  **Flappy Bird Game**
    *   **Description:** A clone of the simple yet challenging "Flappy Bird" mobile game.
    *   **Implementations:**
        *   Gemini Pro 2.5: [`gemini_pro_2.5/FlappyBird/`](./gemini_pro_2.5/FlappyBird/)
        *   GPT-4o: [`gpt4-o/GPTFlappyBird/`](./gpt4-o/GPTFlappyBird/)

## 🛠️ Getting Started / Usage

**Prerequisites:**

*   **Android Studio:** The official IDE for Android development. [Download here](https://developer.android.com/studio)
*   **Java Development Kit (JDK):** Android Studio usually bundles or helps install a compatible version.
*   **Android SDK:** Installed via Android Studio's SDK Manager.

**Steps:**

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/your-repo-name.git
    cd your-repo-name
    ```

2.  **Open a Specific App Project in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open" or "Open an Existing Project".
    *   Navigate to and select the directory of the *specific application* you want to run (e.g., `your-repo-name/gemini_pro_2.5/WeatherApp` or `your-repo-name/gpt4-o/GPTGame2048`).
    *   **Do NOT open the root repository folder or the LLM folder (`gemini_pro_2.5`, `gpt4-o`) as the project.**

3.  **Sync Gradle:** Android Studio should prompt you to sync the project's Gradle files. If not, click `File > Sync Project with Gradle Files`. This downloads dependencies.

4.  **Build and Run:**
    *   Select a target device (emulator or physical device connected via USB).
    *   Click the "Run" button (green play icon) in Android Studio.
    *   The IDE will build the `.apk` and install it on the target device.

## 🤝 Contributing

Contributions are welcome! Help us expand this comparison:

*   **Add New App Implementations:** Generate code for *new* common Android app ideas using the featured LLMs (Gemini Pro 2.5, GPT-4o).
*   **Add Implementations from Other LLMs:** Generate versions of the *existing* apps using different LLMs (e.g., Claude 3, Llama 3) following the established directory structure (`[llm_name]/[AppName]/`).
*   **Fix Build/Runtime Issues:** LLM-generated code often has bugs or build configuration errors. Submit Pull Requests to fix critical issues that prevent an app from building or running. Clearly document the changes made from the original LLM output.
*   **Add Prompts:** If you contribute an implementation, please include the `prompt.md` file detailing the prompt used.
*   **Improve Documentation:** Enhance this README or add specific instructions within app directories.
*   **Report Issues:** Found a bug, build error, or inconsistency? Open an issue!

Please follow standard GitHub fork & pull request workflows.

## 📜 License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.


## ⚠️ Disclaimer

The code in this repository is primarily generated by AI (LLMs) and is intended for **experimental, comparative, and educational purposes only**.

*   **Not Production Ready:** This code likely contains bugs, security vulnerabilities, inefficient algorithms, and may not follow all Android development best practices.
*   **Functionality Varies:** The degree to which each app fulfills its intended purpose may differ significantly between LLM implementations.

**Use this repository as a tool for learning about and comparing LLM code generation for Android, not as a source of reliable, production-grade applications without extensive review, testing, and modification.**