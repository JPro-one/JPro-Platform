# JPro File
A library for handling file related operations (like open, save and drag & drop) independently of the running platform,
either desktop/mobile or on the web via `JPro`. These are the major components currently implemented to fulfil these
requirements:

- `FileOpenPicker`: Allows the users to browse and select files from their file system via a dialog. We can specify
file extension filters to guide users in choosing the right type of files and enable single or multiple file selections.
Once the user confirm their choice, a provided callback handler is invoked for further processing.

    * Key Features
        1. **File extension filtering**: The picker allows to specify an array or list of acceptable file extensions,
        making it easier for users to find the type of files they want to select.
        2. **Multiple File Selection**: Depending on the application needs, we can configure the picker to allow users
        to select either a single file or multiple files.
        3. **Callback Handler**: Once the user selects the file(s), a provided handler function is invoked.

    * Usage Example

      ```java
      ExtensionFilter textExtensionFilter = ExtensionFilter.of("Subtitle files", ".txt", ".srt", ".vtt");
      ExtensionFilter videoExtensionFilter = ExtensionFilter.of("Video files", ".mp4", ".avi", ".mkv");
      
      FileOpenPicker fileOpenPicker = FileOpenPicker.create(openButton);
      fileOpenPicker.getExtensionFilters().addAll(textExtensionFilter, videoExtensionFilter);
      fileOpenPicker.setSelectedExtensionFilter(videoExtensionFilter);
      fileOpenPicker.setSelectionMode(SelectionMode.MULTIPLE);
      fileOpenPicker.setOnFilesSelected(fileSources -> openFiles(fileSources));
      ```

- `FileSavePicker`: Assists the user in saving files to their file system. We can set supported file types, default
file names and starting directories. A callback handler is invoked to specify the saving tasks upon the successfully
selected file. For the `Web` implementation this picker acts as a file downloader.

    * Key Features
        1. **File Type Support**: We can specify which file types are supported for saving, guiding the user to choose
        an appropriate file extension.
        2. **Default File Name & Location**: The picker allows us to set a default file name and starting directory,
        making it convenient for the user.
        3. **Callback Handler**: Upon successful file choosing, a handler function is triggered, enabling us to execute
        the saving tasks.

    * Usage Example

      ```java
      FileSavePicker fileSavePicker = FileSavePicker.create(saveButton);
      fileSavePicker.setInitialFileName("subtitle");
      fileSavePicker.setSelectedExtensionFilter(ExtensionFilter.of("Subtitle format (.srt)", ".srt"));
      fileSavePicker.setOnFileSelected(file -> saveTask().apply(file));
      ```

- `FileDropper`: A file dropper provide handlers for mouse drag & dropping events in order to drop file into a defined
area. It supports file extension filtering and can handle single or multiple files. Once files are successfully dropped,
a specified callback handler is invoked for further processing.

    * Key Features
        1. **File Extension Filtering**: We can set a list of acceptable file extensions, ensuring that only relevant
        files can be dropped into the specified node.
        2. **Multiple File Support**: The dropper can be configured to accept single or multiple files, providing 
        flexibility for various use-cases.
        3. **Visual Feedback**: A specified node can be styled and configured to provide visual cues when a file is
        being dragged over it, or when an incompatible file type is attempted.
        4. **Callback Handler**: Upon successful file(s) drop, a handler function we provided is invoked, allowing us
        to define how these files should be processed.

    * Usage Example

      ```java
      FileDropper fileDropper = FileDropper.create(dropPane);
      fileDropper.setExtensionFilter(ExtensionFilter.of("Subtitle files", ".txt", ".srt", ".vtt"));
      fileDropper.setOnDragEntered(event ->
          dropPane.pseudoClassStateChanged(FILES_DRAG_OVER_PSEUDO_CLASS, true));
      fileDropper.setOnDragExited(event ->
              dropPane.pseudoClassStateChanged(FILES_DRAG_OVER_PSEUDO_CLASS, false));
      fileDropper.setOnFilesSelected(fileSources -> openFiles(fileSources));
      ```

### Installation
- Gradle
    ```groovy
    dependencies {
        implementation("one.jpro.platform:jpro-file:0.2.17-SNAPSHOT")
    }
    ```
- Maven
    ```xml
    <dependencies>
      <dependency>
        <groupId>one.jpro.platform</groupId>
        <artifactId>jpro-file</artifactId>
        <version>0.2.17-SNAPSHOT</version>
      </dependency>
    </dependencies>
    ```

### Launch the examples
[**Text Editor sample**](https://github.com/JPro-one/jpro-platform/blob/jpro-file-module/jpro-file/example/src/main/java/one/jpro/platform/file/example/editor/TextEditorSample.java)
* As desktop application
  ```shell
  ./gradlew jpro-file:example:run -Psample=text-editor
  ```
* As JPro application
  ```shell
  ./gradlew jpro-file:example:jproRun -Psample=text-editor
  ```

[**File Uploader sample**](https://github.com/JPro-one/jpro-platform/blob/jpro-file-module/jpro-file/example/src/main/java/one/jpro/platform/file/example/upload/FileUploaderSample.java)
* As desktop application
  ```shell
  ./gradlew jpro-file:example:run -Psample=file-uploader
  ```
* As JPro application
  ```shell
  ./gradlew jpro-file:example:jproRun -Psample=file-uploader
  ```