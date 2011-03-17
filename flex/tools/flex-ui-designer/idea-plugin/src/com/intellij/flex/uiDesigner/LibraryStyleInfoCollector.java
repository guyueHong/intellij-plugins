package com.intellij.flex.uiDesigner;

import com.intellij.flex.uiDesigner.io.ByteArrayOutputStreamEx;
import com.intellij.flex.uiDesigner.io.PrimitiveAmfOutputStream;
import com.intellij.flex.uiDesigner.io.StringRegistry;
import com.intellij.javascript.flex.css.FlexStyleIndex;
import com.intellij.javascript.flex.css.FlexStyleIndexInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.css.CssFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LibraryStyleInfoCollector {
  private final Project project;
  private final PsiDocumentManager psiDocumentManager;
  private final Module module;

  private final PrimitiveAmfOutputStream bytes = new PrimitiveAmfOutputStream(new ByteArrayOutputStreamEx(128));
  private final CssWriter cssWriter;
  private final StringRegistry.StringWriter stringWriter;

  public LibraryStyleInfoCollector(Project project, Module module, StringRegistry.StringWriter stringWriter) {
    this.project = project;
    this.psiDocumentManager = PsiDocumentManager.getInstance(project);
    this.module = module;
    this.stringWriter = stringWriter;
    cssWriter = new CssWriter(this.stringWriter);
  }

  private byte[] collectInherited(final VirtualFile jarFile) {
    bytes.getByteArrayOut().allocate(2);
    
    final VirtualFile libraryFile = jarFile.findChild("library.swf");
    assert libraryFile != null;
    
    final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
    final GlobalSearchScope searchScope = GlobalSearchScope.fileScope(project, libraryFile);
    final THashSet<String> uniqueGuard = new THashSet<String>();
    fileBasedIndex.processAllKeys(FlexStyleIndex.INDEX_ID, new Processor<String>() {
      @Override
      public boolean process(String dataKey) {
        fileBasedIndex.processValues(FlexStyleIndex.INDEX_ID, dataKey, libraryFile, new FileBasedIndex.ValueProcessor<Set<FlexStyleIndexInfo>>() {
          @Override
          public boolean process(VirtualFile file, Set<FlexStyleIndexInfo> value) {
            final FlexStyleIndexInfo firstInfo = value.iterator().next();
            if (firstInfo.getInherit().charAt(0) == 'y' && uniqueGuard.add(firstInfo.getAttributeName())) {
              bytes.writeUInt29(stringWriter.getReference(firstInfo.getAttributeName()) - 1);
            }
            
            // Если в библиотеке определено свойство — то мы его считаем уникальным для всей библиотеки — мы делаем допущение, что не может быть у одного класса stylePName быть inherited, а у другого класса этой же библиотеки not inherited
            return false;
          }
        }, searchScope);
        
        return true;
      }
    }, project);
    
    if (uniqueGuard.size() == 0) {
      bytes.reset();
      return null;
    }
    else {
      bytes.putShort(uniqueGuard.size(), 0);
      byte[] result = bytes.getByteArrayOut().toByteArray();
      bytes.reset();
      return result;
    }
  }

  public void collect(final @NotNull OriginalLibrary library) {
    library.inheritingStyles = collectInherited(library.getFile()); 
    
    VirtualFile defaultsCssVirtualFile = library.getDefaultsCssFile();
    if (defaultsCssVirtualFile == null) {
      return;
    }

    Document document = FileDocumentManager.getInstance().getDocument(defaultsCssVirtualFile);
    CssFile cssFile = (CssFile) psiDocumentManager.getPsiFile(document);
    assert cssFile != null;
    // need for activate FlexCssElementDescriptorProvider
    cssFile.putUserData(ModuleUtil.KEY_MODULE, module);
    library.defaultsStyle = cssWriter.write(cssFile, document, module);
  }
}