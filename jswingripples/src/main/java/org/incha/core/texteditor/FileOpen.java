package org.incha.core.texteditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.incha.ui.texteditor.TextEditor;

import javax.swing.*;
import java.io.*;
import java.util.HashMap;

/**
 * Created by rodrigo on 05-05-2016.
 */
public class FileOpen {
    private RSyntaxTextArea text;
    private String path;
    private String extension;
    private String content;
    private HashMap<String,String> extensionMap = new HashMap<>();

    public FileOpen( String absolutePath ) {
        path = absolutePath;
        text = new RSyntaxTextArea();
    }

    public boolean open() {
        try {
            BufferedReader reader = new BufferedReader( new FileReader( path ) );
            StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while ( line != null ) {
                sb.append( line + "\n" );
                line = reader.readLine();
            }

            // See if we can set its highlighting
            if (path.indexOf('.') != -1) {
                extension = path.substring(path.lastIndexOf('.') + 1);
                setSyntax(getSyntax(extension));
            }
            else{
                extension = "";
            }
            content = sb.toString();
            text.setText(content);
            return true;

        } catch ( Exception e ) {
            e.printStackTrace();
            return false;
        }
    }

    private String getSyntax(String extension) {
        extensionMap.put( "c", SyntaxConstants.SYNTAX_STYLE_C );
        extensionMap.put( "cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS );
        extensionMap.put( "cs", SyntaxConstants.SYNTAX_STYLE_CSHARP );
        extensionMap.put( "html", SyntaxConstants.SYNTAX_STYLE_HTML );
        extensionMap.put( "java", SyntaxConstants.SYNTAX_STYLE_JAVA );
        extensionMap.put( "js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT );
        extensionMap.put( "php", SyntaxConstants.SYNTAX_STYLE_PHP );
        extensionMap.put( "py", SyntaxConstants.SYNTAX_STYLE_PYTHON );
        extensionMap.put( "rb", SyntaxConstants.SYNTAX_STYLE_RUBY );
        extensionMap.put( "sql", SyntaxConstants.SYNTAX_STYLE_SQL );
        extensionMap.put( "xml", SyntaxConstants.SYNTAX_STYLE_XML );
        return extensionMap.get(extension);
    }

    public void save(JFrame frame) {
        BufferedWriter output = null;
        try{
            output = new BufferedWriter( new FileWriter( path ) );
            output.write( text.getText() );
            JOptionPane.showMessageDialog(frame,
                    "File Saved.");

        } catch( IOException e ){
            e.printStackTrace();
        } finally {
            try {
                output.close();
            }catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
    public String getContent() {
        return content;
    }

    public String getExtension() {
        return extension;
    }

    public void setSyntax(String syntax) {
        text.setSyntaxEditingStyle(syntax);
    }

    public RSyntaxTextArea getText() {
        return text;
    }

    public String getPath(){
        return path;
    }

    public String getFileName(){
        return path.substring( path.lastIndexOf( "/" ) + 1 );
    }
}

