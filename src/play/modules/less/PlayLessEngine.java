package play.modules.less;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import org.mozilla.javascript.WrappedException;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;

import play.Logger;

/**
 * LessEngine wrapper for Play
 */
public class PlayLessEngine {
    LessEngine lessEngine;
    DynamicImportsResolver resolver;
    Boolean devMode;

    PlayLessEngine(Boolean devMode) {
        lessEngine = new LessEngine();
        resolver = new DynamicImportsResolver(devMode);
        this.devMode = devMode;
    }

    public String compile(File lessFile) {
        try {
            // If there are dynamic imports to be resolved, the resolver will
            // return the less file to be compiled
            File dynamicLessFile = resolver.resolveImports(lessFile);
            if (dynamicLessFile != null) {
                return lessEngine.compile(dynamicLessFile);
            } else {
                // Otherwise just compile the file normally
                return lessEngine.compile(lessFile);
            }
        } catch (LessException e) {
            return handleException(lessFile, e);
        }
    }

    public String handleException(File lessFile, LessException e) {
        Logger.warn(e, "Less exception");

        String filename = e.getFilename();
        List<String> extractList = e.getExtract();
        String extract = null;
        if (extractList != null) {
            extract = extractList.toString();
        }

        // LessEngine reports the file as null when it's not an @imported file
        if (filename == null) {
            filename = lessFile.getName();
        }

        // Try to detect missing imports (flaky)
        if (extract == null && e.getCause() instanceof WrappedException) {
            WrappedException we = (WrappedException) e.getCause();
            if (we.getCause() instanceof FileNotFoundException) {
                FileNotFoundException fnfe = (FileNotFoundException) we.getCause();
                extract = fnfe.getMessage();
            }
        }

        return formatMessage(filename, e.getLine(), e.getColumn(), extract, e.getErrorType());
    }

    public String formatMessage(String filename, int line, int column, String extract,
            String errorType) {
        return "body:before {display: block; color: #c00; white-space: pre; font-family: monospace; background: #FDD9E1; border-top: 1px solid pink; border-bottom: 1px solid pink; padding: 10px; content: \"[LESS ERROR] "
                + String.format("%s:%s: %s (%s)", filename, line, extract, errorType) + "\"; }";
    }
}
