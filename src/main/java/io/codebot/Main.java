package io.codebot;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws GitAPIException {
        Path originFolder = Paths.get("D:\\repo\\projectbasic");
        Path targetFolder = Paths.get("D:\\codebot\\projectbasic");
        setupProjectFolder(originFolder, targetFolder, "codebot");

        MavenLauncher launcher = new MavenLauncher(
                targetFolder.toString(),
                MavenLauncher.SOURCE_TYPE.APP_SOURCE
        );
        launcher.buildModel();
        CtModel model = launcher.getModel();

        // list all packages of the model
        for (CtPackage p : model.getAllPackages()) {
            System.out.println("package: " + p.getQualifiedName());
        }
        // list all classes of the model
        for (CtType<?> s : model.getAllTypes()) {
            System.out.println("class: " + s.getQualifiedName());
        }
    }

    private static void setupProjectFolder(Path origin, Path target, String branchName)
            throws GitAPIException {

        try (Git git = Git.cloneRepository()
                .setURI(origin.toUri().toString())
                .setDirectory(target.toFile())
                .call()) {
            if (git.branchList().call().stream().noneMatch(it -> it.getName().equals(branchName))) {
                git.branchCreate()
                        .setName(branchName)
                        .setStartPoint("master")
                        .call();
            }
            git.checkout()
                    .setName(branchName)
                    .call();
        }
    }
}
