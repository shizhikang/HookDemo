package com.example.asm;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ASMPlugin implements Plugin<Project> {

    void apply(Project project) {
        def log = project.logger
        log.error "========================";
        log.error "ASM开始修改Class!";
        log.error "========================";
        project.android.registerTransform(new ASMTransform(project))
    }
}