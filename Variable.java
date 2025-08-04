    import java.util.ArrayList;
    import java.util.List;

    public class Variable {
        private final String name;
        private final List<Variable> parents;
        private final List<String> outcomes;
        private Factor variableFactor;

        public Variable(String name, List<String> outcomes, List<Variable> parents) {
            this.name = name;
            this.outcomes = outcomes;
            this.parents = parents;
        }



        // Method to create the factor (CPT) for this variable, given a list of probabilities
        public void createFactor(List<Double> probabilities) {
            this.variableFactor = new Factor(this, this.parents, probabilities);
        }

        // Getter method to retrieve the name of the variable
        public String getName() {
            return this.name;
        }

        // Getter method to retrieve the list of parent variables
        public List<Variable> getParents() {
            return this.parents;
        }

        // Getter method to retrieve the names of the parent variables
        public List<String> getParentsNames() {
            List<String> parentsNames = new ArrayList<>();
            for (Variable v : this.getParents()) {
                parentsNames.add(v.getName());
            }
            return parentsNames;
        }

        // Getter method to retrieve the factor (CPT) for this variable
        public Factor getFactor() {

            return this.variableFactor;
        }

        // Getter method to retrieve the outcomes of this variable
        public List<String> getOutcomes() {

            return this.outcomes;
        }
    }
