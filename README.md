# SafeGitUploader

> ✅ A robust cross-platform CLI tool engineered in Java for secure, filtered Git uploads from local directories such as SD cards, USB drives, or any file system path. It intelligently ignores malformed filenames, hidden/system files, and ensures safe commits to a remote Git repository.

---

## 📦 Features

- ✅ Recursively scans source directory
- ✅ Ignores system trash (`.DS_Store`, `Thumbs.db`, `._*`, `.*`, `_MACOSX`)
- ✅ Filters invalid or malformed folder/file names
- ✅ Generates `.gitignore` automatically for Java projects
- ✅ Supports dry-run mode (audit before upload)
- ✅ Logs skipped files to `upload.log`
- ✅ Cross-platform (macOS, Linux, Windows)
- ✅ Fully production-ready with enhanced error handling
- ✅ Lightweight, zero dependencies

---

## 🚀 Usage

### 1. 🧑‍💻 Compile

```bash
javac SafeGitUploader.java
