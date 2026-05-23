package compilerTools;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    public String label;
    public String value;
    public List<ASTNode> children = new ArrayList<>();

    public ASTNode(String label) {
        this.label = label;
    }

    public ASTNode(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public void addChild(ASTNode child) {
        children.add(child);
    }
}
