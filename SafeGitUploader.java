/*
 * Copyright © 2024 Devin B. Royal.
 * All Rights Reserved.
 */

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class SafeGitUploader {

    private static final List<String> IGNORED_PATTERNS = Arrays.asList(
            ".DS_Store", "Thumbs.db", "_MACOSX", "(?i)^\\._.*", "(?i)^\\..*"
    );

    private static final String GITIGNORE_CONTENT = String.join(System.lineSeparator(),
            ".DS_Store",
            "Thumbs.db",
            "*.log",
            "target/",
            "bin/",
            "*.class",
            ".idea/",
            ".vscode/",
            "*.iml"
    );

    private static final String LOG_FILE = "upload.log";

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);

        if (!params.containsKey("--source") || !params.containsKey("--remote") || !params.containsKey("--branch")) {
            System.err.println("Usage: java -jar SafeGitUploader.jar --source <path> --remote <git-url> --branch <branch-name> [--dry-run]");
            System.exit(1);
        }

        Path sourceDir = Paths.get(params.get("--source")).toAbsolutePath();
        String gitRemote = params.get("--remote");
        String branch = params.get("--branch");
        boolean dryRun = params.containsKey("--dry-run");

        try {
            validateGitInstalled();
            generateGitIgnore(sourceDir);
            List<Path> validFiles = new ArrayList<>();
            scanDirectory(sourceDir, validFiles);

            if (dryRun) {
                System.out.println("[Dry Run] Files to be committed:");
                validFiles.forEach(System.out::println);
                return;
            }

            initGitRepo(sourceDir, gitRemote, branch);
            addFilesToGit(sourceDir, validFiles);
            commitAndPush(sourceDir, branch);

        } catch (Exception e) {
            e.printStackTrace();
            log("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if ((i + 1) < args.length && !args[i + 1].startsWith("--")) {
                    map.put(args[i], args[i + 1]);
                    i++;
                } else {
                    map.put(args[i], "true");
                }
            }
        }
        return map;
    }

    private static void validateGitInstalled() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "--version")
                .redirectErrorStream(true)
                .start();
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("Git is not installed or not found in PATH.");
        }
    }

    private static void generateGitIgnore(Path sourceDir) throws IOException {
        Path gitignore = sourceDir.resolve(".gitignore");
        if (!Files.exists(gitignore)) {
            Files.write(gitignore, GITIGNORE_CONTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void scanDirectory(Path root, List<Path> validFiles) throws IOException {
        Files.walk(root).forEach(path -> {
            try {
                if (Files.isRegularFile(path) && isValidFilename(path.getFileName().toString())) {
                    validFiles.add(path);
                } else {
                    log("Skipped: " + path.toString());
                }
            } catch (Exception e) {
                log("Error checking path: " + path.toString());
            }
        });
    }

    private static boolean isValidFilename(String filename) {
        for (String pattern : IGNORED_PATTERNS) {
            if (Pattern.compile(pattern).matcher(filename).matches()) {
                return false;
            }
        }
        return true;
    }

    private static void initGitRepo(Path dir, String remote, String branch) throws IOException, InterruptedException {
        if (!Files.exists(dir.resolve(".git"))) {
            runCommand(dir, "git", "init");
            runCommand(dir, "git", "checkout", "-b", branch);
            runCommand(dir, "git", "remote", "add", "origin", remote);
        }
    }

    private static void addFilesToGit(Path root, List<Path> files) throws IOException, InterruptedException {
        for (Path file : files) {
            runCommand(root, "git", "add", root.relativize(file).toString());
        }
    }

    private static void commitAndPush(Path dir, String branch) throws IOException, InterruptedException {
        runCommand(dir, "git", "commit", "-m", "Safe upload of clean files");
        runCommand(dir, "git", "push", "-u", "origin", branch);
    }

    private static void runCommand(Path dir, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new IOException("Command failed: " + String.join(" ", cmd));
        }
    }

    private static void log(String message) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(LOG_FILE), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(new Date() + ": " + message);
            writer.newLine();
        } catch (IOException ignored) {
        }
    }
}
