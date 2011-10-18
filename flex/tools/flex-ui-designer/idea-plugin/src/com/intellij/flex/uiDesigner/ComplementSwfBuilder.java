package com.intellij.flex.uiDesigner;

import com.intellij.flex.uiDesigner.abc.*;
import com.intellij.flex.uiDesigner.libraries.FlexOverloadedClasses;
import com.intellij.flex.uiDesigner.libraries.FlexOverloadedClasses.InjectionClassifier;
import com.intellij.openapi.util.text.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ComplementSwfBuilder {
  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      // build all
      build(args[0], "4.1", InjectionClassifier.framework);
      build(args[0], "4.1", InjectionClassifier.spark, false);
      build(args[0], "4.5", InjectionClassifier.framework);
      build(args[0], "4.5", InjectionClassifier.spark, false);
    }
    else if (args.length == 3) {
      build(args[0], args[1], InjectionClassifier.valueOf(args[1]));
    }
    else {
      throw new IllegalArgumentException("Usage: ComplementSwfBuilder <folder> [<flexVersion> <classifier>]");
    }
  }

  public static void build(String rootPath, String flexVersion, InjectionClassifier classifier) throws IOException {
    build(rootPath, flexVersion, classifier, true);
  }

  private static void build(String rootPath, String flexVersion, InjectionClassifier classifier, boolean buildComplement) throws IOException {
    final AbcNameFilter sparkInclusionNameFilter = new AbcNameFilterStartsWith("com.intellij.flex.uiDesigner.flex", true);

    final Collection<CharSequence> commonDefinitions = new ArrayList<CharSequence>(1);
    commonDefinitions.add("com.intellij.flex.uiDesigner:SpecialClassForAdobeEngineers");

    final AbcNameFilter air4InclusionNameFilter = new AbcNameFilterByNameSet(FlexOverloadedClasses.AIR_SPARK_CLASSES, true);
    // SpriteLoaderAsset must be loaded with framework.swc, because _s000 located in framework.swc
    File source = getSourceFile(rootPath, flexVersion);

    final AbcNameFilter abcNameFilter;
    if (classifier == InjectionClassifier.framework) {
      abcNameFilter = new AbcNameFilterByNameSetAndStartsWith(commonDefinitions,
                                                              new String[]{"mx.", "spark."}) {
        @Override
        public boolean accept(CharSequence name) {
          return StringUtil.equals(name, "com.intellij.flex.uiDesigner.flex:SpriteLoaderAsset") ||
                 StringUtil.equals(name, FlexOverloadedClasses.STYLE_PROTO_CHAIN) ||
                 FlexOverloadedClasses.MX_CLASSES.contains(name) ||
                 (super.accept(name) && !sparkInclusionNameFilter.accept(name) &&
                  !air4InclusionNameFilter.accept(name));
        }
      };
    }
    else {
      abcNameFilter = new AbcNameFilterByEquals(FlexOverloadedClasses.SKINNABLE_COMPONENT);
    }

    new AbcFilter(null).filter(source, createAbcFile(rootPath, flexVersion, classifier), abcNameFilter);

    if (buildComplement) {
      new AbcFilter(null).filter(source, new File(rootPath + "/complement-flex" + flexVersion + ".swf"), sparkInclusionNameFilter);
      new AbcFilter(null).filter(source, new File(rootPath + "/complement-air4.swf"), air4InclusionNameFilter);
    }
  }

  public static File getSourceFile(String folder, String flexVersion) {
    return new File(folder, "flex-injection-" + flexVersion + "-1.0-SNAPSHOT.swf");
  }

  public static File createAbcFile(String folder, String flexVersion, InjectionClassifier classifier) {
    return new File(folder, generateInjectionName(flexVersion, classifier));
  }

  public static String generateInjectionName(String flexSdkVersion, InjectionClassifier classifier) {
    return classifier + "-injection-" + flexSdkVersion + ".abc";
  }
}