import java.util.ArrayList;
import java.util.List;

public class VariablePair {
    private final String variableName;
    private final String variableValue;

    public VariablePair(String variableName, String variableValue) {
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    // Getters
    public String getVariableName() {

        return variableName;
    }

    public String getVariableValue() {

        return variableValue;
    }

    //The function returns the possible outcomes of this variable
    public List<String> getOutcomes(List<Variable> variables) {
        Variable var = findVariableByName(variableName, variables);
        if (var != null) {
            return var.getOutcomes();
        }
        return new ArrayList<>();
    }

    //Method to find the variable by his name
    private Variable findVariableByName(String name, List<Variable> variables) {
        for (Variable v : variables) {
            if (v.getName().equals(name)) {
                return v;
            }
        }
        return null;
    }
}
