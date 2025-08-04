
public class Query {

    private final VariablePair queryVariable;
    private final int algorithmType;
    private final VariablePair[] evidence;


    public Query(VariablePair queryVariable, VariablePair[] evidence, int algorithmType) {
        this.queryVariable = queryVariable;
        this.evidence = evidence;
        this.algorithmType = algorithmType;
    }

    // Getter method to retrieve the query variable
    public VariablePair getQueryVariable () {
        return this.queryVariable;
    }

    // Getter method to retrieve the evidence (the known variable-value pairs)
    public VariablePair[] getEvidence() {
        return this.evidence;
    }

    // Getter method to retrieve the algorithm type
    public int getAlgorithmType() {
        return this.algorithmType;
    }
}
