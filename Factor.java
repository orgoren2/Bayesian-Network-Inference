import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Factor {


    private final Map<List<String>, Double> cpt = new HashMap<>();

    public Factor(Variable targetVariable, List<Variable> variableParents, List<Double> probabilities) {

        // Create combinations for cpt variables - parents and target variable
        List<Variable> allVars = new ArrayList<>(variableParents);
        allVars.add(targetVariable);
        List<List<String>> cptCombinations = generateAllCombinations(allVars);

        // Fill map
        populateCpt(cpt, cptCombinations, probabilities, 1);
    }

    private void populateCpt(Map<List<String>, Double> cptMap, List<List<String>> combinations, List<Double> probabilities, int step) {
        int index = 0;
        for (List<String> combination : combinations) {
            cptMap.put(combination, probabilities.get(index));
            index += step;
        }
    }

    public Map<List<String>, Double> getCpt() {

        return this.cpt;
    }

    // Function to generate all combinations of outcomes for a given list of variables
    private List<List<String>> generateAllCombinations(List<Variable> variables) {
        List<List<String>> result = new ArrayList<>();
        generateRecursive(variables, 0, new ArrayList<>(), result);
        return result;
    }

    // Recursive helper to build all combinations
    private void generateRecursive(List<Variable> vars, int idx, List<String> current, List<List<String>> result) {
        if (idx == vars.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (String val : vars.get(idx).getOutcomes()) {
            current.add(val);
            generateRecursive(vars, idx + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
