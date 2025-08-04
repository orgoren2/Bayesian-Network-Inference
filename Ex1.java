import java.io.*;
import java.util.*;

public class Ex1 {


    // The function parses every row from input into a Query object
    public static Query parseQuery(String line) {
        // Find the closing parenthesis of the probability expression
        int closingParenIndex = line.indexOf(')');
        String queryPartFull = line.substring(0, closingParenIndex + 1).trim();

        // Default algorithm type is 0
        int algorithmType = 0;
        if (line.length() > closingParenIndex + 1) {
            String afterParen = line.substring(closingParenIndex + 1).trim();
            if (afterParen.startsWith(",")) {
                algorithmType = Integer.parseInt(afterParen.substring(1).trim());
            }
        }

        // Remove 'P(' from the start and ')' from the end of the probability expression
        String inside = queryPartFull.substring(2, queryPartFull.length() - 1);

        VariablePair queryVariable;
        VariablePair[] evidence;

        String[] parts;
        if (inside.contains("|")) {
            parts = inside.split("\\|"); // Separate query from evidence using '|'
        } else {
            parts = new String[]{inside}; // No evidence present
        }

        // First part always contains the query variable (and possibly some evidence)
        String[] assignments = parts[0].split(",");
        String[] querySplit = assignments[0].split("=");
        queryVariable = new VariablePair(querySplit[0].trim(), querySplit[1].trim());

        // Determine the size of the evidence array
        int evidenceIndex = 0;
        if (parts.length > 1) {
            // Conditional probability — evidence is the part after '|'
            String[] conds = parts[1].split(",");
            evidence = new VariablePair[conds.length + assignments.length - 1]; // Includes extra conditions before '|'

            // Add evidence before the '|' (if any, besides the query variable)
            for (int i = 1; i < assignments.length; i++) {
                String[] ev = assignments[i].split("=");
                evidence[evidenceIndex++] = new VariablePair(ev[0].trim(), ev[1].trim());
            }

            // Add evidence after the '|'
            for (String cond : conds) {
                String[] ev = cond.trim().split("=");
                evidence[evidenceIndex++] = new VariablePair(ev[0].trim(), ev[1].trim());
            }

        } else {
            // Joint probability — no conditional evidence
            evidence = new VariablePair[assignments.length - 1];

            for (int i = 1; i < assignments.length; i++) {
                String[] ev = assignments[i].split("=");
                evidence[evidenceIndex++] = new VariablePair(ev[0].trim(), ev[1].trim());
            }
        }

        return new Query(queryVariable, evidence, algorithmType);
    }


    // Calculates the joint probability of a given query

    public static double[] calculateJointProbability(Query query, List<Variable> variables) {
        int numOfMultiplicationOperations = 0, numOfAdditionOperations = 0;
        double totalProbability = 1.0;

        // Initialize an array to hold the query variable and evidence variables
        VariablePair[] q = new VariablePair[variables.size()];
        q[0] = query.getQueryVariable();

        for (int i = 0; i < query.getEvidence().length; i++) {
            q[i+1] = query.getEvidence()[i];

        }


        // Iterate over all variables in q
        for (int i = 0; i < q.length; i++) {
            Variable currentVariable = null;

            // Find the corresponding Variable object from the list
            for (Variable v : variables) {
                if (q[i].getVariableName().equals(v.getName())) {
                    currentVariable = v;
                    break;
                }
            }

            assert currentVariable != null;

            // Build a list of values for the current variable's CPT
            List<String> values = new ArrayList<>();

            // Add the values of all parent variables, in order
            for (Variable parent : currentVariable.getParents()) {
                for (VariablePair vp : q) {
                    if (parent.getName().equals(vp.getVariableName())) {
                        values.add(vp.getVariableValue());
                        break;
                    }
                }
            }

            // Add the value of the current variable itself
            values.add(q[i].getVariableValue());

            // Lookup the probability in the CPT
            double prob = currentVariable.getFactor().getCpt().get(values);

            // Multiply into the total probability
            totalProbability *= prob;

            // Count multiplication operations (ignore the first multiplication)
            if (i > 0) {
                numOfMultiplicationOperations++;
            }
        }


        return new double[]{totalProbability, numOfMultiplicationOperations};
    }


    //The function gets the results from calculateJointProbability and return it
    public static String returnJointProbabilityCalculate(Query query, List<Variable> variables) {
        double[] result = calculateJointProbability(query, variables);
        return String.format("%.5f,%d,%d", result[0], 0, (int)result[1]);
    }


    //  The function checks if the query variable factor  is the wanted factor
    // by checking if the evidence variables match exactly the parents of the query variable
    public static double isOriginalFactor(Query query, List<Variable> variables, Variable queryVar){
        List<String> queryEvidenceVariables = new ArrayList<>();
        List<String> queryEvidenceValues = new ArrayList<>();
        // Collect the names and values of evidence variables
        for (int i = 0; i < query.getEvidence().length; i++) {
            queryEvidenceVariables.add(query.getEvidence()[i].getVariableName());
            queryEvidenceValues.add(query.getEvidence()[i].getVariableValue());
        }

        // Add the query variable's own value to the evidence values
        queryEvidenceValues.add(query.getQueryVariable().getVariableValue());
        // If the evidence variables match exactly the parents of the query variable
        if (queryEvidenceVariables.equals(queryVar.getParentsNames())) {
            return queryVar.getFactor().getCpt().get(queryEvidenceValues);}

        return 0.0;
    }

    public static String simpleConditionalIndependenceCalculation(Query query, List<Variable> variables) {

        VariablePair[] evidenceVariables = query.getEvidence();
        Variable queryVar = null;

        // Find the query variable in the list of variables
        for (Variable v : variables) {
            if (v.getName().equals(query.getQueryVariable().getVariableName())) {
                queryVar = v;
                break;
            }
        }
        //If the evidence variable size equals the size of the query variable parents
        if(queryVar.getParentsNames().size()==query.getEvidence().length) {
            //Check if the query variable factor is the wanted factor
            //if it does, return the probability directly from the factor
            double probability = isOriginalFactor(query, variables, queryVar);
            if (probability != 0.0) {
                //The number of multiplication and addition operaion is zero because
                // we didn't compute anything, we took whe probability straight from the factor
                return String.format("%.5f", probability) + "," + (0) + "," + 0;
            }
        }

        // Create the list of hidden variables
        List<Variable> hiddenVariables = new ArrayList<>();
        createHiddenVariablesList(query, variables, hiddenVariables, evidenceVariables);

        // Collect all possible outcomes for each hidden variable
        List<List<String>> hiddenOutcomes = new ArrayList<>();
        for (Variable var : hiddenVariables) {
            hiddenOutcomes.add(var.getOutcomes());
        }

        // Generate all combinations of hidden variable outcomes
        List<List<String>> combinations = generateAllCombinationsFromOutcomes(hiddenOutcomes);

        // Prepare a new evidence array that will include original evidence + hidden variable values combinations
        VariablePair[] newEvidence = new VariablePair[evidenceVariables.length + hiddenVariables.size()];
        //Insert the evidence variables and their values to the list
        for (int i = 0; i < evidenceVariables.length; i++) {
            newEvidence[i] = evidenceVariables[i];
        }

        int numOfAdditionOperations = 0;
        int numOfMultiplicationOperations = 0;
        double numerator = 0.0;
        double denominator = 0.0;

        // Get all possible outcomes for the query variable
        List<String> queryVariableOutcomes = query.getQueryVariable().getOutcomes(variables);

        boolean first = true;

        // Iterate over all combinations of hidden variables
        for (List<String> combo : combinations) {
            int index = 0;

            // Add hidden variable values as evidence
            for (int j = evidenceVariables.length; j < newEvidence.length; j++) {
                newEvidence[j] = new VariablePair(hiddenVariables.get(index).getName(), combo.get(index));
                index++;
            }

            // For each outcome of the query variable, calculate the joint probability
            for (String value : queryVariableOutcomes) {
                VariablePair newQueryVar = new VariablePair(query.getQueryVariable().getVariableName(), value);

                // Calculate the joint probability for this combination
                double[] result = calculateJointProbability(new Query(newQueryVar, newEvidence, 0), variables);
                denominator += result[0]; // Sum over all outcomes

                // If this is the outcome we are interested in, add to numerator
                if (value.equals(query.getQueryVariable().getVariableValue())) {
                    numerator += result[0];
                }

                numOfMultiplicationOperations += (int) result[1];
            }

            // Count additions (once per combination after the first)
            if (first) {
                // on the very first combo we need to count k–1 denominator additions
                numOfAdditionOperations += queryVariableOutcomes.size() - 1;
                first = false;
            } else {
                // on every subsequent combo we count k denominator additions
                numOfAdditionOperations += queryVariableOutcomes.size();
            }

        }

        // Return the final probability and the number of operations
        return String.format("%.5f", numerator / denominator) + "," + (numOfAdditionOperations ) + "," + numOfMultiplicationOperations;
    }



    public static void createHiddenVariablesList(Query query, List<Variable> variables, List<Variable> hiddenVariables,
                                                 VariablePair[] evidenceVariables) {

        VariablePair queryVariable = query.getQueryVariable();
        //Iterate through all the variables
        //If the variable name equals the query or evidence variable name continue. Otherwise, insert it to the list.
        for (Variable var : variables) {
            String varName = var.getName();

            if (varName.equals(queryVariable.getVariableName())) {
                continue;
            }
            boolean isEvidence = false;
            for (VariablePair evidence : evidenceVariables) {
                if (varName.equals(evidence.getVariableName())) {
                    isEvidence = true;
                    break;
                }
            }
            if (!isEvidence) {
                hiddenVariables.add(var);
            }
        }
    }

    public static List<List<String>> generateAllCombinationsFromOutcomes(List<List<String>> outcomesList) {
        List<List<String>> result = new ArrayList<>();
        // Start the recursive generation of combinations starting from depth 0
        generateRecursiveFromOutcomes(outcomesList, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateRecursiveFromOutcomes(List<List<String>> outcomesList, int depth,
                                                      List<String> current, List<List<String>> result) {

        //  If depth reaches the size of outcomesList, add the current combination to the result
        if (depth == outcomesList.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Loop over each value in the current list of outcomes at the current depth
        for (String val : outcomesList.get(depth)) {
            current.add(val); // Add the value to the current combination
            // Recurse to the next depth with the updated combination
            generateRecursiveFromOutcomes(outcomesList, depth + 1, current, result);
            // Backtrack by removing the last added value to explore other combinations
            current.remove(current.size() - 1);
        }
    }

    public static String variableEliminationCalculate(Query query, List<Variable> variables, boolean isHeuristic) {
        Map<List<String>, Map<List<String>, Double>> variableToCpt = new HashMap<>();
        int numOfAdditionOperations = 0, numOfMultiplicationOperations = 0;
        int index = 0;

        Variable queryVar = null;

        // Find the query variable in the list of variables
        for (Variable v : variables) {
            if (v.getName().equals(query.getQueryVariable().getVariableName())) {
                queryVar = v;
                break;
            }
        }
        //If the evidence variable size equals the size of the query variable parents
        if(queryVar.getParentsNames().size()==query.getEvidence().length) {
            //Check if the query variable factor is the wanted factor
            //if it does, return the probability directly from the factor
            double probability = isOriginalFactor(query, variables, queryVar);
            if (probability != 0.0) {
                return String.format("%.5f", probability) + "," + (0) + "," + 0;
            }
        }

        //Initialize the variableToCpt map
        for (Variable var : variables) {
            List<String> cptVariables = new ArrayList<>(var.getParentsNames());
            cptVariables.add(var.getName());
            //Add symbol to distinguish between the cpt because it's not possible to insert
            // few values with the same key into the map
            cptVariables.add(0, "#" + index++);
            Map<List<String>, Double> copiedCpt = new HashMap<>();
            for (Map.Entry<List<String>, Double> entry : var.getFactor().getCpt().entrySet()) {
                copiedCpt.put(new ArrayList<>(entry.getKey()), entry.getValue());
            }
            variableToCpt.put(cptVariables, copiedCpt);
        }

        //Create hidden variables list
        List<String> hiddenVariables = createHiddenList(query, variables);
        //Create list of the variables who are not ancestors of the evidence or query variables
        List<String> ignoreVariables = createIgnoreVariablesList(query, variables, hiddenVariables);
        List<List<String>> keysToRemove = new ArrayList<>();
        //If one of the ignore variables is part of the cpt variables, remove the cpt from variableToCpt
        for (List<String> key : variableToCpt.keySet()) {
            for (String ignore : ignoreVariables) {
                if (key.contains(ignore)) {
                    keysToRemove.add(key);
                }
            }
        }
        for (List<String> key : keysToRemove) {
            variableToCpt.remove(key);
        }
        // Reduce the map by the evidence variables boolean values
        reduceCptsByEvidence(variableToCpt, query.getEvidence(), variables);
        //Eliminate the evidence variables from the cpts
        eliminateEvidenceFromCpts(variableToCpt, query.getEvidence());

        //Iterate through all the hidden variables
        while (!hiddenVariables.isEmpty()) {
            //Check if the function call was heuristic or not
            String currentHidden = isHeuristic ? chooseHiddenVariableHeuristic(variableToCpt, hiddenVariables) : hiddenVariables.get(0);

            List<Map<List<String>, Double>> relevantFactors = new ArrayList<>();
            List<List<String>> variablesInKey = new ArrayList<>();
            //Create a list of all the factors that contains currentHidden
            for (List<String> key : variableToCpt.keySet()) {
                if (key.contains(currentHidden)) {
                    relevantFactors.add(variableToCpt.get(key));
                    variablesInKey.add(key);
                }
            }
            //Remove the collected factors from variableToCpt
            for (List<String> key : variablesInKey) {
                variableToCpt.remove(key);
            }

            //Make join on the currentHidden factors
            numOfMultiplicationOperations += joinFactors(relevantFactors, variablesInKey, currentHidden);
            //Eliminate currentHidden from the factor result
            numOfAdditionOperations += eliminateHiddenVariable(relevantFactors, variablesInKey, currentHidden);

            //If the map wasn't the hidden value factor
            if (variablesInKey.get(0).size() > 1) {
                variableToCpt.put(variablesInKey.get(0), relevantFactors.get(0));
            }
            //Remove currentHidden from the hiddenVariables list
            hiddenVariables.remove(currentHidden);

        }

        Map<List<String>, Double> finalMap = new HashMap<>();
        //Check if there is more than one factor left in variable to cpt
        if (variableToCpt.size() > 1) {
            List<Map<List<String>, Double>> relevantFactors = new ArrayList<>(variableToCpt.values());
            //Make join to the left factors
            numOfMultiplicationOperations += joinQueryFactors(query, relevantFactors, variables);
            finalMap = relevantFactors.get(0);

        } else {
            //If there is only one factor left
            for (List<String> key : variableToCpt.keySet()) {
                if (key.contains(query.getQueryVariable().getVariableName())) {
                    finalMap = variableToCpt.get(key);

                }
            }
        }

        double sum = 0.0, finalProbability = 0.0;
        // Variable to track if it's the first iteration
        boolean first = true;

        // Loop through each key in the finalMap
        for (List<String> key : finalMap.keySet()) {
            // If this is the first entry, add the first value to sum without incrementing the numOfAdditionOperaions
            if (first) {
                sum += finalMap.get(key); // Add the value to the sum
                first = false; // Set first as false
            } else {
                sum += finalMap.get(key); // Add the value in subsequent iterations
                numOfAdditionOperations++;
            }

            // If the query variable matches the key's first value, store the corresponding probability
            if (query.getQueryVariable().getVariableValue().equals(key.get(0))) {
                finalProbability = finalMap.get(key);
            }
        }

        // Calculate the normalized probability using the sum
        finalProbability /= sum;

        return String.format("%.5f", finalProbability) + "," + numOfAdditionOperations + "," + numOfMultiplicationOperations;
    }

    public static String chooseHiddenVariableHeuristic(Map<List<String>, Map<List<String>, Double>> variableToCpt,
                                                       List<String> hiddenVariables) {

        // Build a graph: each variable points to a set of variables it shares a factor with
        Map<String, Set<String>> variableGraph = new HashMap<>();
        // Map to track smallest CPT size for each variable
        Map<String, Integer> cptSize = new HashMap<>();

        //Iterate through all the keys in variableToCpt
        for (Map.Entry<List<String>, Map<List<String>, Double>> entry : variableToCpt.entrySet()) {
            List<String> factorVars = entry.getKey();
            int tableSize = entry.getValue().size();

            // Connect all variables in the factor to each other
            for (String var1 : factorVars) {
                variableGraph.putIfAbsent(var1, new HashSet<>());
                // record smallest CPT size for var1
                cptSize.put(var1, Math.min(cptSize.getOrDefault(var1, Integer.MAX_VALUE), tableSize));
                for (String var2 : factorVars) {
                    if (!var1.equals(var2)) {
                        variableGraph.get(var1).add(var2);
                    }
                }
            }
        }

        // Choose the hidden variable with the minimum degree (fewest neighbors)
        // Tie-breaker: smallest CPT size
        String bestHidden = null;
        int minDegree = Integer.MAX_VALUE;
        int bestTableSize = Integer.MAX_VALUE;

        for (String hidden : hiddenVariables) {
            Set<String> neighbors = variableGraph.getOrDefault(hidden, new HashSet<>());
            int degree = neighbors.size();
            int tableSize = cptSize.getOrDefault(hidden, Integer.MAX_VALUE);

            if (degree < minDegree || (degree == minDegree && tableSize < bestTableSize)) {
                minDegree = degree;
                bestTableSize = tableSize;
                bestHidden = hidden;
            }
        }

        return bestHidden;
    }


    public static List<String> createIgnoreVariablesList(Query query, List<Variable> variables, List<String> hiddenVariables) {
        List<String> ignoreVariables = new ArrayList<>();

        for (Variable v : variables) {
            String varName = v.getName();

            //Check if the hiddenVariables contains the variable
            boolean isHidden = false;
            for (String hidden : hiddenVariables) {
                if (varName.equals(hidden)) {
                    isHidden = true;
                    break;
                }
            }

            // Check if evidence variable list contains the variable
            boolean isEvidence = false;
            for (VariablePair vp : query.getEvidence()) {
                if (varName.equals(vp.getVariableName())) {
                    isEvidence = true;
                    break;
                }
            }

            //If the variable is not evidence, query or hidden, insert it to the list
            if (!varName.equals(query.getQueryVariable().getVariableName()) && !isHidden && !isEvidence) {
                ignoreVariables.add(varName);
            }
        }
        return ignoreVariables;
    }


    public static int eliminateHiddenVariable(List<Map<List<String>, Double>> relevantFactors,
                                              List<List<String>> variablesInKey, String hiddenVarName) {

        // Find the index of the hidden variable in the key (skip the first element, which is the #index tag)
        int hiddenIndex = variablesInKey.get(0).subList(1, variablesInKey.get(0).size()).indexOf(hiddenVarName);
        Map<List<String>, Double> cpt = relevantFactors.get(0);

        Map<List<String>, Double> reducedCpt = new HashMap<>();
        Set<List<String>> processedKeys = new HashSet<>();
        int numOfAdditionOperation = 0;

        // Iterate over each key in the CPT
        for (List<String> key : new ArrayList<>(cpt.keySet())) {
            // Skip if already processed
            if (processedKeys.contains(key)) continue;

            // Remove the hidden variable's value from the current key to form the reduced key
            List<String> reducedKey = new ArrayList<>(key);
            reducedKey.remove(hiddenIndex);

            double sumProbability = 0.0;
            boolean isFirst = true;

            // Sum over all entries with the same reduced key
            for (Map.Entry<List<String>, Double> entry : cpt.entrySet()) {
                List<String> currentKey = entry.getKey();
                List<String> reducedCurrent = new ArrayList<>(currentKey);
                reducedCurrent.remove(hiddenIndex);

                // If reduced keys match, add the probabilities
                if (reducedCurrent.equals(reducedKey)) {
                    sumProbability += entry.getValue();
                    processedKeys.add(currentKey); // Mark as processed
                    // Only count additions after the first
                    if (!isFirst) {
                        numOfAdditionOperation++;
                    }
                    isFirst = false;
                }
            }

            // Store the final probability for the reduced key
            reducedCpt.put(reducedKey, sumProbability);
        }

        // Replace the old CPT with the reduced one
        cpt.clear();
        cpt.putAll(reducedCpt);

        // Update the variable list by removing the hidden variable
        variablesInKey.get(0).remove(hiddenIndex + 1); // +1 because of the "#index" prefix

        return numOfAdditionOperation;
    }

    public static int joinQueryFactors(Query query, List<Map<List<String>, Double>> relevantFactors, List<Variable> variables) {
        int numOfMultiplicationOperation = 0;

        // Continue joining the factors until only one factor remains
        while (relevantFactors.size() > 1) {
            Map<List<String>, Double> factor1 = relevantFactors.get(0);
            Map<List<String>, Double> factor2 = relevantFactors.get(1);
            Map<List<String>, Double> resultMap = new HashMap<>();

            // Iterate over the entries of the first factor
            for (Map.Entry<List<String>, Double> entry1 : factor1.entrySet()) {
                List<String> outcome1 = entry1.getKey();
                // The probability associated with the outcome in factor1
                Double prob1 = entry1.getValue();
                // The probability associated with the same outcome in factor2
                Double prob2 = factor2.get(outcome1);

                // If the same outcome exists in both factors, multiply their probabilities
                if (prob2 != null) {
                    double resultProbability = prob1 * prob2;
                    resultMap.put(outcome1, resultProbability);
                    numOfMultiplicationOperation++;
                }
            }

            // Add the new joined factor to the list and remove the two processed factors
            relevantFactors.add(resultMap);
            relevantFactors.remove(1);
            relevantFactors.remove(0);
        }

        return numOfMultiplicationOperation;
    }


    public static int joinFactors(List<Map<List<String>, Double>> relevantFactors, List<List<String>> variablesInKey, String hiddenVarName) {
        int numOfMultiplicationOperation = 0;


        //Sort relevantFactors and variablesInKey from the minimum size of the table to maximum size of table
        for (int i = 0; i < relevantFactors.size() - 1; i++) {
            for (int j = i + 1; j < relevantFactors.size(); j++) {
                if (relevantFactors.get(i).size() > relevantFactors.get(j).size()) {
                    //Change maps location
                    Map<List<String>, Double> tempFactor = relevantFactors.get(i);
                    relevantFactors.set(i, relevantFactors.get(j));
                    relevantFactors.set(j, tempFactor);

                    //Change variables location
                    List<String> tempVars = variablesInKey.get(i);
                    variablesInKey.set(i, variablesInKey.get(j));
                    variablesInKey.set(j, tempVars);
                }
            }
        }

        // Continue joining until one factor remains
        while (relevantFactors.size() > 1) {
            Map<List<String>, Double> factor1 = relevantFactors.get(0);
            Map<List<String>, Double> factor2 = relevantFactors.get(1);
            Map<List<String>, Double> resultMap = new HashMap<>();


            List<String> overlappingVariables = new ArrayList<>();
            List<Integer> first = new ArrayList<>();
            List<Integer> second = new ArrayList<>();
            //Create a list of all the overlapping variables and add their indexes to the lists
            for (String v1 : variablesInKey.get(0)) {
                for (String v2 : variablesInKey.get(1)) {
                    if (v1.equals(v2)) {
                        overlappingVariables.add(v2);
                        first.add(variablesInKey.get(0).subList(1, variablesInKey.get(0).size()).indexOf(v1));
                        second.add(variablesInKey.get(1).subList(1, variablesInKey.get(1).size()).indexOf(v2));
                        break;
                    }
                }
            }
            int numOfOverlappingVariables = overlappingVariables.size();

            for (Map.Entry<List<String>, Double> entry1 : factor1.entrySet()) {

                for (Map.Entry<List<String>, Double> entry2 : factor2.entrySet()) {
                    List<String> list1 = entry1.getKey(); // Assignment in factor1
                    List<String> list2 = entry2.getKey(); // Assignment in factor2
                    int count = 0;
                    //Check if the overlapping variables has the same value in both factors
                    for (int i = 0; i < numOfOverlappingVariables; i++) {
                        if (list1.get(first.get(i)).equals(list2.get(second.get(i)))) {
                            count++;
                        }
                    }
                    // Only join rows where the overlapping variables has the same value in both factors
                    if (count == numOfOverlappingVariables) {
                        // Merge the variable lists
                        List<String> mergedList = mergeAssignments(list1, list2, variablesInKey);

                        // Multiply the probabilities
                        double probability = entry1.getValue() * entry2.getValue();
                        numOfMultiplicationOperation++;

                        // Add the merged list and probability to the new factor
                        resultMap.put(mergedList, probability);
                    }
                }
            }

            // Remove the joined factors from the list
            relevantFactors.remove(1);
            relevantFactors.remove(0);
            // Add the new combined factor
            relevantFactors.add(resultMap);

            // Merge the variable names
            List<String> mergedVars = new ArrayList<>(variablesInKey.get(0));
            for (int i = 1; i < variablesInKey.get(1).size(); i++) {
                String var = variablesInKey.get(1).get(i);
                if (!mergedVars.contains(var)) {
                    mergedVars.add(var);
                }
            }

            // Update the variable names list by remove old ones and add the new merged one
            variablesInKey.remove(1);
            variablesInKey.remove(0);
            variablesInKey.add(mergedVars);
        }

        return numOfMultiplicationOperation;
    }


    private static List<String> mergeAssignments(List<String> list1, List<String> list2,
                                                 List<List<String>> variablesInKey) {
        List<String> mergedAssignment = new ArrayList<>(list1);

        List<String> vars1 = variablesInKey.get(0).subList(1, variablesInKey.get(0).size());
        List<String> vars2 = variablesInKey.get(1).subList(1, variablesInKey.get(1).size());

        for (int i = 0; i < list2.size(); i++) {
            String varName = vars2.get(i);
            //If the variable doesn't exist on the first list, we will insert his value
            if (!vars1.contains(varName)) {
                mergedAssignment.add(list2.get(i));
            }
        }

        return mergedAssignment;
    }


    public static void reduceCptsByEvidence(Map<List<String>, Map<List<String>, Double>> variableToCpt, VariablePair[] evidenceList, List<Variable> variables) {

        // Iterate over each evidence variable
        for (VariablePair ev : evidenceList) {
            String hiddenVarName = ev.getVariableName();
            String hiddenVarValue = ev.getVariableValue();

            // For each variable in the network, check if it's affected by this evidence
            for (Variable var : variables) {
                // A variable is affected if the evidence is either the variable itself or one of its parents
                boolean isAffected = var.getName().equals(hiddenVarName) || var.getParentsNames().contains(hiddenVarName);
                if (!isAffected) continue;

                // Construct the key for this variable's CPT (parents + the variable itself)
                List<String> cptVariables = new ArrayList<>(var.getParentsNames());
                cptVariables.add(var.getName());
                List<String> keyCptList = new ArrayList<>();
                for (List<String> key : variableToCpt.keySet()) {
                    if (cptVariables.equals(key.subList(1, key.size()))) {
                        keyCptList = key;
                    }
                }

                // Retrieve the CPT associated with this key
                Map<List<String>, Double> cpt = variableToCpt.get(keyCptList);
                if (cpt == null) continue;

                // Filter out entries from the CPT that do not match the evidence value
                Iterator<Map.Entry<List<String>, Double>> iterator = cpt.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<List<String>, Double> entry = iterator.next();
                    List<String> assignment = entry.getKey();  // The list of T/F values for this CPT row

                    // Find the index of the evidence variable in the assignment list
                    int index = getIndexOfVariableInAssignment(var, hiddenVarName);

                    // If the index is not found or the value doesn't match the evidence, remove the entry
                    if (index == -1 || !assignment.get(index).equals(hiddenVarValue)) {
                        iterator.remove();
                    }
                }
            }
        }
    }


    public static void eliminateEvidenceFromCpts(Map<List<String>, Map<List<String>, Double>> variableToCpt, VariablePair[] evidenceList) {

        // Iterate through each evidence variable
        for (VariablePair ev : evidenceList) {
            String evidenceName = ev.getVariableName();
            List<List<String>> keys = new ArrayList<>(variableToCpt.keySet());

            //Iterate through each key
            for (List<String> cv : keys) {
                Map<List<String>, Double> cpt = variableToCpt.get(cv);
                if (cpt == null) continue;
                // If evidence is the variable itself remove the last cell of the list in the key
                if (cv.get(cv.size() - 1).equals(evidenceName)) {
                    List<String> newCv = new ArrayList<>(cv);

                    //Remove the last cell in the variable list and in all the key lists
                    for (List<String> key : cpt.keySet()) {
                        key.remove(key.size() - 1);
                    }
                    newCv.remove(newCv.size() - 1);
                    variableToCpt.remove(cv);
                    if (newCv.size() > 1) {
                        variableToCpt.put(newCv, cpt);
                    }
                }
                // If evidence is one of the parents remove it from the list in the key
                // and remove its value from the list in the value
                else if (cv.contains(evidenceName)) {
                    int index = cv.indexOf(evidenceName);
                    List<String> newCv = new ArrayList<>(cv);
                    //Remove the cell=index in the variable list and in all the key lists
                    for (List<String> key : cpt.keySet()) {
                        //We remove the index-1 because the cv contains the #tag in the first cell
                        key.remove(index - 1);
                    }
                    newCv.remove(index);
                    Map<List<String>, Double> newCpt = new HashMap<>();
                    for (Map.Entry<List<String>, Double> entry : cpt.entrySet()) {
                        List<String> newAssignment = new ArrayList<>(entry.getKey());
                        newCpt.put(newAssignment, entry.getValue());
                    }

                    variableToCpt.remove(cv);
                    if (newCv.size() > 1) {
                        variableToCpt.put(newCv, newCpt);
                    }
                }
            }
        }
    }


    private static int getIndexOfVariableInAssignment(Variable var, String targetName) {
        // Get the list of parent variable names for the given variable
        List<String> parentNames = var.getParentsNames();
        int index = 0;

        // Iterate over parent names to find the target variable name
        for (String name : parentNames) {
            if (name.equals(targetName)) {
                return index; // Return the index if the target name is a parent
            }
            index++;
        }

        // Check if the target variable name is the variable itself
        if (var.getName().equals(targetName)) {
            return index; // Return the index (last position) if it's the variable itself
        }

        // If the variable was not found, return -1
        return -1;
    }


    private static List<String> createHiddenList(Query query, List<Variable> variables) {

        // Create a list of all variables mentioned in the query (query variable + evidence variables)
        List<String> allQueryVariables = new ArrayList<>();
        allQueryVariables.add(query.getQueryVariable().getVariableName());
        for(VariablePair evidenceVar: query.getEvidence()){
            allQueryVariables.add(evidenceVar.getVariableName());
        }
        // Stack used for traversal of the graph to collect relevant ancestors
        Stack<Variable> stack = new Stack<>();
        for (Variable v : variables) {
            for (String s : allQueryVariables) {
                if (v.getName().equals(s)) {
                    stack.push(v); // Start from the query and evidence variables
                }
            }
        }

        List<String> hiddenList = new ArrayList<>();
        // Track visited variables to avoid duplicates
        Set<String> visited = new HashSet<>();

        // Traverse the Bayesian network upward to find all ancestor variables
        while (!stack.isEmpty()) {
            Variable v = stack.pop();
            String varName = v.getName();
            if (visited.contains(varName)) continue;
            visited.add(varName);

            // Push all parents of the current variable onto the stack for further traversal
            for(Variable tempVariable: variables){
                if(tempVariable.getName().equals(v.getName())) {
                    for (Variable parent : tempVariable.getParents()) {
                        if (!visited.contains(parent.getName())) {

                            stack.push(parent);

                        }
                    }
                }}
            hiddenList.add(varName);
        }

        // Remove query and evidence variables to keep only hidden variables
        hiddenList.removeAll(allQueryVariables);

        // Sort the hidden variable names in alphabetical order
        Collections.sort(hiddenList);

        return hiddenList;
    }




        public static void main(String[] args) {
            try {
                // Read the input file from the project directory
                File file = new File("input.txt");
                BufferedReader reader = new BufferedReader(new FileReader(file));

                // Create output writer
                PrintWriter writer = new PrintWriter(new FileWriter("output.txt"));

                // The first line is the XML file name
                String xmlFileName = reader.readLine();
                XMLParser parser = new XMLParser();
                List<Variable> variables = parser.parseXML(xmlFileName);

                // Reading the queries
                String line;
                while ((line = reader.readLine()) != null) {
                    Query query = parseQuery(line);
                    String result = "";

                    if (query.getAlgorithmType() == 0) {
                         result = returnJointProbabilityCalculate(query, variables);
                    } else if (query.getAlgorithmType() == 1) {
                        result = simpleConditionalIndependenceCalculation(query, variables);
                    } else if (query.getAlgorithmType() == 2) {
                        result = variableEliminationCalculate(query, variables, false);
                    } else if (query.getAlgorithmType() == 3) {
                        result = variableEliminationCalculate(query, variables, true);
                    }

                    writer.println(result); // Write the result to the output file
                }

                reader.close();
                writer.close(); // Close the output file

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


