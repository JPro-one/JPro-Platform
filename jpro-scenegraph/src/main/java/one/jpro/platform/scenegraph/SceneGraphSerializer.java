package one.jpro.platform.scenegraph;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;

import java.util.List;

/**
 * This class serializes a scene graph to a string representation.
 * This String is both human and AI friendly.
 * @author floriankirmaier
 */
public class SceneGraphSerializer {

    private List<String> importantClasses = List.<String>of(
            "javafx.scene.layout.VBox",
            "javafx.scene.layout.HBox",
            "javafx.scene.layout.StackPane",
            "javafx.scene.layout.GridPane",
            "javafx.scene.control.ScrollPane"
    );

    private List<String> finalClasses = List.<String>of(
            "javafx.scene.control.Label",
            "javafx.scene.control.Button"
    );

    /**
     * Serializes a scene graph to a string representation.
     * This String is both human and AI friendly.
     * @param node
     * @return
     */
    public static String serialize(Node node) {
        return new SceneGraphSerializer().serialize(node, 0);
    }

    private String serialize(Node node, int depth) {
        if(node == null) {
            return "";
        }
        var clazz = getNamedClass(node.getClass());
        var layoutClass = getLayoutClass(clazz);

        var sb = new StringBuilder();
        sb.append(getIndent(depth));

        sb.append(clazz.getSimpleName());
        if(layoutClass != null && !layoutClass.equals(clazz)) {
            sb.append("(");
            sb.append(layoutClass.getSimpleName());
            sb.append(")");
        }

        var sbBracket = new StringBuilder();
        var id = node.getId();
        if(id != null && !id.isEmpty()) {
            sbBracket.append("id=");
            sbBracket.append(id);
        }
        for(var styleClass : node.getStyleClass()) {
            if(sbBracket.length() > 0) {
                sbBracket.append(", ");
            }
            sbBracket.append(styleClass);
        }
        if(sbBracket.length() > 0) {
            sb.append(" [");
            sb.append(sbBracket);
            sb.append("]");
        }
        if(node instanceof Labeled labeled && labeled.getText() != null && !labeled.getText().isEmpty()) {
            sb.append(" \"");
            sb.append(escape(labeled.getText()));
            sb.append("\"");
        }
        sb.append("\n");
        if(!isFinalClass(clazz)) {
            if(node instanceof Parent) {
                for(var child : ((Parent) node).getChildrenUnmodifiable()) {
                    sb.append(serialize(child, depth + 1));
                }
            }
        }


        return sb.toString();
    }

    private String getIndent(int depth) {
        var sb = new StringBuilder();
        for(int i = 0; i < depth; i++) {
            sb.append("- ");
        }
        return sb.toString();
    }


    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    // Anonymous classes have an empty simple name.
    private Class<?> getNamedClass(Class<?> clazz) {
        while(clazz.getSimpleName().isEmpty()) {
            clazz = clazz.getSuperclass();
        }
        return clazz;
    }

    private Class<?> getLayoutClass(Class<?> obj) {
        if(obj == null) {
            return null;
        }
        if(importantClasses.contains(obj.getName())) {
            return obj;
        }
        return getLayoutClass(obj.getSuperclass());
    }

    private boolean isFinalClass(Class<?> obj) {
        if(obj == null) {
            return false;
        }
        if(finalClasses.contains(obj.getName())) {
            return true;
        }
        return isFinalClass(obj.getSuperclass());
    }
}
