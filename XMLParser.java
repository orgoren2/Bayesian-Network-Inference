import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLParser {
    private final Map<String, Variable> variableMap = new HashMap<>();

    public List<Variable> parseXML(String filename) {
        try {
            File xmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Map to temporarily store outcomes
            Map<String, List<String>> outcomeMap = new HashMap<>();

            // First pass: read <VARIABLE> elements to extract name and outcomes
            NodeList variableNodes = doc.getElementsByTagName("VARIABLE");
            for (int i = 0; i < variableNodes.getLength(); i++) {
                Element variableElement = (Element) variableNodes.item(i);

                String varName = variableElement.getElementsByTagName("NAME").item(0).getTextContent();
                List<String> outcomes = new ArrayList<>();
                NodeList outcomeNodes = variableElement.getElementsByTagName("OUTCOME");
                for (int j = 0; j < outcomeNodes.getLength(); j++) {
                    outcomes.add(outcomeNodes.item(j).getTextContent());
                }

                outcomeMap.put(varName, outcomes);
                // Create variable stub with no parents yet
                variableMap.put(varName, new Variable(varName, outcomes, new ArrayList<>()));
            }

            // Second pass: handle definitions (parents + CPTs)
            NodeList definitionNodes = doc.getElementsByTagName("DEFINITION");
            for (int i = 0; i < definitionNodes.getLength(); i++) {
                Element defElement = (Element) definitionNodes.item(i);

                String varName = defElement.getElementsByTagName("FOR").item(0).getTextContent();
                List<String> outcomes = outcomeMap.get(varName);

                NodeList givenNodes = defElement.getElementsByTagName("GIVEN");
                List<Variable> parents = new ArrayList<>();
                for (int j = 0; j < givenNodes.getLength(); j++) {
                    String parentName = givenNodes.item(j).getTextContent();
                    parents.add(variableMap.get(parentName));
                }

                // Rebuild the variable with parents
                Variable currentVariable = new Variable(varName, outcomes, parents);
                variableMap.put(varName, currentVariable);

                // Parse CPT values
                String[] probsStr = defElement.getElementsByTagName("TABLE").item(0).getTextContent().trim().split("\\s+");
                List<Double> probabilities = new ArrayList<>();
                for (String prob : probsStr) {
                    probabilities.add(Double.parseDouble(prob));
                }

                // Create factor and assign it to the variable
                currentVariable.createFactor(probabilities);
            }

            return new ArrayList<>(variableMap.values());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
