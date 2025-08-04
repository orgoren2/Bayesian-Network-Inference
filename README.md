# Bayesian Network Inference

This Java project implements probabilistic inference in Bayesian networks using the Variable Elimination algorithm.  
It supports parsing a custom XML format representing the network structure and conditional probability tables (CPTs),  
and computes probabilistic queries given in an input file.

## Features

- Parses Bayesian network structure from XML
- Supports two types of queries:
  - Joint probability queries: `P(E1=e1, E2=e2, ..., En=en)`
  - Conditional probability queries: `P(Q=q | E1=e1, ..., Ek=ek)`
- Implements Variable Elimination for probabilistic inference
- Supports both fixed and heuristic variable elimination orders
- Outputs results to a plain text file

---

## Input Format

1. **XML file** (`network.xml`) – defines the variables, their outcomes, and the CPTs.
2. **Query file** (`input.txt`) – contains a list of queries to compute, one per line.

---

## Output Format

The program writes the result of each query to a file named `output.txt`.  
Each line corresponds to the result of one query, in the same order as in the input file.

---

## How to Run

1. **Place your files**:
   - Save your Bayesian network XML file as `network.xml` (or another name).
   - Save your queries in `input.txt`.

2. **Update the XML file name in code**:  
   Open `Ex1.java` and set the correct file name:
   ```java
   String xmlFile = "network.xml"; // Update this if needed
   ```
   
3. **Compile the code**:
```bash
javac *.java
```

4. **Run the program**:
```bash
java Ex1
```

5. **Check the results**:
The output will appear in output.txt.

---

## Notes
The program is written in plain Java (no external libraries required).
There is no GUI – input and output are handled through text files.
The expected XML format follows the assignment specification, including <VARIABLE>, <DEFINITION>, <GIVEN>, and <TABLE> tags.

---

### License
This project was developed as part of an academic assignment and is provided for educational use.









