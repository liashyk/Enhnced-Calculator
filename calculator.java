import java.util.ArrayList;
import java.util.HashMap;


public class Assignment11Part1 {

    /**
     * interface for math functions
     */
    public interface IAction {
        double calculate(double number);
    }

    /**
     * Map, where key is name of function, and value is method reference
     */
    static HashMap<String, IAction> functionMap;

    /**
     * List, that contain spltted formula
     */
    static ArrayList<String> previousFormulaList;

    /**
     * Formula value from previous run of program
     */
    static String prevFormula;


    public static void main(String[] args) {
        initMap();
        runCalculator(args);
    }

    /**
     * Initialize map, where key is name of function, and value is method reference
     */
    private static void initMap() {
        functionMap = new HashMap<>();
        functionMap.put("sin", Math::sin);
        functionMap.put("cos", Math::cos);
        functionMap.put("tan", Math::tan);
        functionMap.put("atan", Math::atan);
        functionMap.put("log10", Math::log10);
        functionMap.put("log2", (a) -> Math.log10(a) / Math.log10(2));
        functionMap.put("sqrt", Math::sqrt);
    }

    /**
     * Write result of calculation. Take arguments from main
     */
    public static void runCalculator(String[] args) {
        try {
            checkArgs(args);
            System.out.println("result: " + calculate(args[0], makeMap(args)));
        } catch (Exception exception) {
            System.out.print("ERROR: ");
            System.out.println(exception.getMessage());
        }
    }

    /**
     * Throw error if args is empty or have only one item with only operator
     */
    static void checkArgs(String[] args) throws Exception {
        if (args.length == 0) {
            throw new Exception("There is nothing to calculate");
        } else if (args.length == 1 && args[0].length() == 1 && isOperator(args[0].charAt(0))) {
            throw new Exception("Program can't calculate formula with only operator");
        }
    }

    /**
     * Return result of calculation in Double type.
     * Take formula, that contain in args array in index 0, and HashMap of variables
     */
    public static double calculate(String formula, HashMap<String, Double> variables) throws Exception {
        //delete all spaces from formula
        formula = formula.replaceAll(" ", "");
        System.out.println(formula);
        if (!formula.equals(prevFormula)) {
            //split formula on list
            previousFormulaList = splitFormula(formula);
            prevFormula = formula;
        }
        //insert variables from HashMap to formula
        ArrayList<String> formulaList = new ArrayList<>(previousFormulaList);
        replaceVariables(formulaList, variables);
        if (isUnknownVariable(formulaList)) {
            throw new Exception("There is unknown variable. Advise: functions like 'sin' must have brackets, " +
                    "because otherwise it will be read as variable  ");
        }
        //pass list with formula through all calculations
        double result = passThroughOperators(formulaList);
        return result;
    }

    /**
     * Return true if list have unknown variable
     */
    private static boolean isUnknownVariable(ArrayList<String> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!(isOperator(list.get(i).charAt(0)) || isNumber(list.get(i)) || isFunction(list.get(i)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if map with function has ket that equal this string in parameters
     */
    private static boolean isFunction(String s) {
        return functionMap.containsKey(s);
    }

    /**
     * pass list with formula through all pass list with formula through all calculations.
     * Return double result of all calculation
     */
    private static double passThroughOperators(ArrayList<String> list) throws Exception {
        try {
            passThroughBrackets(list);
            makeNegativeDigits(list);
            passThroughFunctions(list);
            passTroughPow(list);
            passTroughMultiplyAndDivision(list);
            passTroughPlusAndMinus(list);
            if (list.size() == 1) {
                return Double.parseDouble(list.get(0));
            }
        } catch (NumberFormatException exception) {
            throw new Exception("There are left more than 1 number after all operations. " +
                    "Maybe there are a lot of operators without numbers");
        }
        return -1;
    }

    /**
     * Pass through all function and replace him by value of this function.
     * Example:log2(256)+2-->8+2
     */
    private static void passThroughFunctions(ArrayList<String> list) throws Exception {
        for (int i = 0; i < list.size(); i++) {
            if (isFunction(list.get(i))) {
                String functionName = list.get(i);
                if (isNumber(list.get(i + 1))) {
                    double number = Double.parseDouble(list.get(i + 1));
                    list.set(i, String.valueOf(functionMap.get(functionName).calculate(number)));
                    list.remove(i + 1);
                }
            }
        }
    }

    /**
     * Replace all list formula's brackets with value in this brackets.
     * Example: 2*(2+2)->2*4
     */
    private static void passThroughBrackets(ArrayList<String> list) throws Exception {
        int startBracketAmount = 0;
        int startBracketIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals("(")) {
                if (startBracketIndex < 0) {
                    startBracketIndex = i;
                }
                startBracketAmount++;
            } else if (list.get(i).equals(")")) {
                startBracketAmount--;
                if (startBracketAmount == 0) {
                    replaceBrackets(list, startBracketIndex, i);
                    startBracketIndex = -1;
                } else if (startBracketAmount < 0) {
                    throw new Exception("Something wrong with brackets");
                }
            }
        }
    }


    /**
     * Take list with start and end indexes of brackets, that havo to be replaces.
     * Replace this bracket by new value
     */
    private static void replaceBrackets(ArrayList<String> list, int start, int end) throws Exception {
        int expectedSize = list.size() - (end - start);
        ArrayList<String> sublist = new ArrayList<>(list.subList(start + 1, end));
        double newValue = passThroughOperators(sublist);
        list.set(start, String.valueOf(newValue));
        while (list.size() > expectedSize) {
            list.remove(start + 1);
        }
    }


    /**
     * Pass through list and transform two elements
     * "-" and "number" to one "-number" element if "-" element isn't between two numbers
     */
    private static void makeNegativeDigits(ArrayList<String> list) {
        String next = "";
        double buffer;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals("-") && !isNumbersNear(list, i)) {
                if (i < list.size() - 1)
                    next = list.get(i + 1);
                if (isNumber(next)) {
                    buffer = Double.parseDouble(next) * -1;
                    list.set(i, String.valueOf(buffer));
                    list.remove(i + 1);
                }
            }
        }
    }

    /**
     * Return true if element at index in list is between numbers
     */
    private static boolean isNumbersNear(ArrayList<String> list, int index) {
        try {
            return isNumber(list.get(index - 1)) && isNumber(list.get(index + 1));
        } catch (IndexOutOfBoundsException exception) {
            return false;
        }
    }

    /**
     * Return true if line can be parsed to Double
     */
    private static boolean isNumber(String line) {
        try {
            Double.parseDouble(line);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate all "+" and "-" operators in formula
     */
    private static void passTroughPlusAndMinus(ArrayList<String> list) {
        String buffer;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals("+") || list.get(i).equals("-")) {
                if (list.get(i).equals("+")) {
                    buffer = String.valueOf(sum(list.get(i - 1), list.get(i + 1)));
                } else {
                    buffer = String.valueOf(subtract(list.get(i - 1), list.get(i + 1)));
                }
                list.set(i - 1, buffer);
                list.remove(i + 1);
                list.remove(i);
                i -= 2;
                if (i < 0) i = 0;
            }
        }
    }

    /**
     * Return sum of two elements
     */
    private static double sum(String s, String s1) {
        double num1 = Double.parseDouble(s);
        double num2 = Double.parseDouble(s1);
        return num1 + num2;
    }

    /**
     * Return subtraction of two elements
     */
    private static double subtract(String s, String s1) {
        double num1 = Double.parseDouble(s);
        double num2 = Double.parseDouble(s1);
        return num1 - num2;
    }

    /**
     * Calculate all "^" operators in formula
     */
    private static void passTroughPow(ArrayList<String> list) throws Exception {
        String buffer;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals("^")) {
                buffer = String.valueOf(pow(list.get(i - 1), list.get(i + 1)));
                list.set(i - 1, buffer);
                list.remove(i + 1);
                list.remove(i);
                i -= 2;
            }
        }
    }

    /**
     * Calculate all "*" and "/" operators in formula
     */
    private static void passTroughMultiplyAndDivision(ArrayList<String> list) {
        String buffer;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals("*") || list.get(i).equals("/")) {
                if (list.get(i).equals("*")) {
                    buffer = String.valueOf(multiplication(list.get(i - 1), list.get(i + 1)));
                } else {
                    buffer = String.valueOf(division(list.get(i - 1), list.get(i + 1)));
                }
                list.set(i - 1, buffer);
                list.remove(i + 1);
                list.remove(i);
                i -= 2;
            }
        }
    }

    /**
     * Return division of two elements
     */
    private static double division(String s, String s1) {
        double num1 = Double.parseDouble(s);
        double num2 = Double.parseDouble(s1);
        return num1 / num2;
    }

    /**
     * Return multiplication of two elements
     */
    private static double multiplication(String s, String s1) {
        double num1 = Double.parseDouble(s);
        double num2 = Double.parseDouble(s1);
        return num1 * num2;
    }

    /**
     * Return s to exponent s1. Throw exception if s1 isn't integer
     */
    private static double pow(String s, String s1) throws Exception {
        double base = Double.parseDouble(s);
        double exponent = Double.parseDouble(s1);
        return Math.pow(base, exponent);

    }

    /**
     * Replace variables in formula by values in HashMap
     */
    private static void replaceVariables(ArrayList<String> list, HashMap<String, Double> variables) {
        for (int i = 0; i < list.size(); i++) {
            if (variables.containsKey(list.get(i))) {
                list.set(i, String.valueOf(variables.get(list.get(i))));
            }
        }
    }

    /**
     * Split formula into ArrayList. For instance
     * "23+1" turn into array {"23","+","1"}
     */
    private static ArrayList<String> splitFormula(String formula) {
        ArrayList<String> list = new ArrayList<>();
        String buffer = "";
        for (int i = 0; i < formula.length(); i++) {
            if (isOperator(formula.charAt(i))) {
                if (!buffer.equals("")) {
                    list.add(buffer);
                }
                list.add(String.valueOf(formula.charAt(i)));
                buffer = "";
            } else {
                buffer = buffer.concat(String.valueOf(formula.charAt(i)));
            }
        }
        if (!buffer.equals("")) {
            list.add(buffer);
        }
        return list;
    }

    /**
     * Return true if letter is operator
     */
    static boolean isOperator(char letter) {
        char[] operators = {'+', '-', '*', '/', '^', '(', ')'};
        for (int i = 0; i < operators.length; i++) {
            if (letter == operators[i]) {
                return true;
            }
        }
        return false;
    }


    /**
     * Make HashMap of variables, that contain in args[] array
     */
    public static HashMap<String, Double> makeMap(String[] args) throws Exception {
        HashMap<String, Double> map = new HashMap<>();
        String[] buffer;
        for (int i = 1; i < args.length; i++) {
            buffer = args[i].replaceAll(" ", "").split("=");
            if (isFunction(buffer[0])) {
                throw new Exception("Variable can't be named like function (sin, cos, tan, atan, log10, log2, sqrt)");
            }
            map.put(buffer[0], Double.valueOf(buffer[1]));
        }
        return map;
    }
}
