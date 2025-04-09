package com.sanad.gemini_2_dot_5_pro_preview.scientificcalculator;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView displayTextView;
    StringBuilder currentInput; // Use StringBuilder for efficient string manipulation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = findViewById(R.id.displayTextView);
        displayTextView.setMovementMethod(new ScrollingMovementMethod()); // Enable scrolling
        currentInput = new StringBuilder();

        // Set onClickListener for all buttons programmatically
        int[] buttonIds = {
                R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4,
                R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9,
                R.id.buttonDot, R.id.buttonAdd, R.id.buttonSubtract, R.id.buttonMultiply,
                R.id.buttonDivide, R.id.buttonEquals, R.id.buttonClear, R.id.buttonBackspace,
                R.id.buttonSin, R.id.buttonCos, R.id.buttonTan, R.id.buttonLog, R.id.buttonLn,
                R.id.buttonSqrt, R.id.buttonPower, R.id.buttonFactorial, R.id.buttonOpenParen,
                R.id.buttonCloseParen, R.id.buttonPercent
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        String buttonText = ((Button) v).getText().toString();

        try { // Wrap logic in try-catch for potential calculation errors
            if (id == R.id.buttonEquals) {
                if (currentInput.length() > 0) {
                    double result = evaluateExpression(currentInput.toString());
                    // Format result nicely (avoid trailing .0)
                    if (result == (long) result) {
                        currentInput = new StringBuilder(String.format("%d", (long) result));
                    } else {
                        currentInput = new StringBuilder(String.format("%s", result));
                    }
                    displayTextView.setText(currentInput.toString());
                }
            } else if (id == R.id.buttonClear) {
                currentInput.setLength(0); // Clear the StringBuilder
                displayTextView.setText("");
            } else if (id == R.id.buttonBackspace) {
                if (currentInput.length() > 0) {
                    // Handle deleting function names like "sin(" correctly
                    int deleteLen = 1;
                    String current = currentInput.toString();
                    if (current.endsWith("sin(") || current.endsWith("cos(") || current.endsWith("tan(")) deleteLen = 4;
                    else if (current.endsWith("log(") || current.endsWith("ln(")) deleteLen = 4;
                    else if (current.endsWith("sqrt(")) deleteLen = 5;

                    currentInput.delete(currentInput.length() - deleteLen, currentInput.length());
                    displayTextView.setText(currentInput.toString());
                }
            } else if (id == R.id.buttonSin || id == R.id.buttonCos || id == R.id.buttonTan ||
                    id == R.id.buttonLog || id == R.id.buttonLn || id == R.id.buttonSqrt) {
                // Append function name with an opening parenthesis
                currentInput.append(buttonText).append("(");
                displayTextView.setText(currentInput.toString());
            }
            // Factorial and Percent are often postfix or require special handling
            else if (id == R.id.buttonFactorial) {
                // Append '!' directly. The evaluator needs to handle it.
                currentInput.append("!");
                displayTextView.setText(currentInput.toString());
            }
            else if (id == R.id.buttonPercent) {
                // Append '%' directly. Evaluator should interpret (e.g., divide preceding number by 100).
                currentInput.append("%");
                displayTextView.setText(currentInput.toString());
            }
            else {
                // Append number, operator, or parenthesis
                // Add spaces around binary operators for easier tokenization (optional but helps)
                if (isBinaryOperator(buttonText) && currentInput.length() > 0) {
                    // Prevent double operators (e.g. "5++", allow "5+-")
                    char lastChar = currentInput.charAt(currentInput.length() - 1);
                    if (!isOperator(String.valueOf(lastChar)) || buttonText.equals("-")) { // Allow negative sign
                        currentInput.append(" ").append(buttonText).append(" ");
                    } else if(isOperator(String.valueOf(lastChar)) && !buttonText.equals("-")) {
                        // Replace last operator if not entering a negative
                        currentInput.delete(currentInput.length() - 2, currentInput.length()); // Remove space + operator
                        currentInput.append(buttonText).append(" ");
                    }
                    // else: ignore the operator input if it's invalid sequence
                } else if (buttonText.equals(".")) {
                    // Prevent multiple dots in one number segment
                    String[] parts = currentInput.toString().split("[\\s()\\+\\-\\*\\/\\^]");
                    if (parts.length > 0 && !parts[parts.length - 1].contains(".")) {
                        currentInput.append(buttonText);
                    } else if (parts.length == 0 && currentInput.length() == 0) {
                        currentInput.append("0."); // Start with 0. if dot is first input
                    } else if (parts.length > 0 && parts[parts.length - 1].isEmpty() && !isOperator(String.valueOf(currentInput.charAt(currentInput.length()-1))) ) {
                        // Case like "5 + " then "." -> "5 + 0."
                        currentInput.append("0.");
                    }
                }
                else {
                    currentInput.append(buttonText);
                }
                displayTextView.setText(currentInput.toString());
            }
        } catch (Exception e) {
            displayTextView.setText("Error");
            currentInput.setLength(0); // Clear input after error
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace(); // Log the full error for debugging
        }
    }


    // --- Calculation Logic ---

    private double evaluateExpression(String expression) {
        try {
            // 1. Tokenize the expression carefully
            List<String> tokens = tokenize(expression.trim()); // Trim whitespace

            // 2. Convert Infix to Postfix (Shunting-Yard)
            List<String> postfix = infixToPostfix(tokens);

            // 3. Evaluate Postfix expression
            return evaluatePostfix(postfix);
        } catch (Exception e) {
            System.err.println("Evaluation Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Invalid Expression"); // Re-throw specific error
        }
    }

    // --- Tokenizer ---
    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentNumber = new StringBuilder();
        StringBuilder currentFunction = new StringBuilder(); // For sin, cos, log etc.

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                if (currentFunction.length() > 0) { // Cannot have number right after function name without '('
                    throw new IllegalArgumentException("Invalid token sequence near function: " + currentFunction);
                }
                currentNumber.append(c);
            } else if (Character.isLetter(c)) {
                if (currentNumber.length() > 0) { // Number followed by letter is invalid unless it's an operator like 'E'
                    tokens.add(currentNumber.toString());
                    currentNumber.setLength(0);
                    // If we support 'E' for scientific notation, handle here
                    // For now, assume functions like sin, cos
                }
                currentFunction.append(c);
                // Check if the function name is complete
                if (i + 1 < expression.length() && expression.charAt(i+1) == '(') {
                    if (isFunction(currentFunction.toString())) {
                        tokens.add(currentFunction.toString());
                        currentFunction.setLength(0);
                    } else {
                        throw new IllegalArgumentException("Unknown function: " + currentFunction);
                    }
                } else if (i + 1 == expression.length() && !isFunction(currentFunction.toString())) {
                    // Reached end but haven't formed a known function
                    throw new IllegalArgumentException("Unknown token: " + currentFunction);
                } // Continue building function name if letters follow

            } else {
                // End of number or function? Add it to tokens.
                if (currentNumber.length() > 0) {
                    tokens.add(currentNumber.toString());
                    currentNumber.setLength(0);
                }
                if (currentFunction.length() > 0) { // Should only happen if function wasn't followed by '(' - error?
                    throw new IllegalArgumentException("Invalid function usage: " + currentFunction);
                    // tokens.add(currentFunction.toString()); // Or treat as variable? Calc doesn't have vars
                    // currentFunction.setLength(0);
                }

                // Handle operators, parentheses, and whitespace
                if (c == ' ') {
                    continue; // Ignore whitespace during tokenization
                } else if (isOperator(String.valueOf(c)) || c == '(' || c == ')') {
                    // Handle unary minus: Check if '-' is at start or follows an operator or '('
                    if (c == '-' && (tokens.isEmpty() || isOperator(tokens.get(tokens.size()-1)) || tokens.get(tokens.size()-1).equals("(")) ) {
                        tokens.add("neg"); // Special token for unary negate
                    } else {
                        tokens.add(String.valueOf(c));
                    }
                } else if (c == '!' || c == '%') { // Postfix operators
                    tokens.add(String.valueOf(c));
                }
                else {
                    throw new IllegalArgumentException("Invalid character in expression: " + c);
                }
            }
        }
        // Add any remaining number or function
        if (currentNumber.length() > 0) {
            tokens.add(currentNumber.toString());
        }
        if (currentFunction.length() > 0) { // Dangling function name?
            throw new IllegalArgumentException("Incomplete function name at end: " + currentFunction);
        }


        System.out.println("Tokens: " + tokens); // Debugging
        return tokens;
    }


    // --- Shunting-Yard Algorithm ---
    private List<String> infixToPostfix(List<String> tokens) {
        List<String> outputQueue = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        for (String token : tokens) {
            if (isNumeric(token)) {
                outputQueue.add(token);
            } else if (isFunction(token)) {
                operatorStack.push(token); // Push functions onto operator stack
            } else if (isOperator(token) || token.equals("neg")) {
                while (!operatorStack.isEmpty() &&
                        !operatorStack.peek().equals("(") &&
                        ((isFunction(operatorStack.peek())) || // Functions have high precedence
                                (getPrecedence(operatorStack.peek()) >= getPrecedence(token)))) {
                    outputQueue.add(operatorStack.pop());
                }
                operatorStack.push(token);
            } else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
                if (operatorStack.isEmpty() || !operatorStack.peek().equals("(")) {
                    throw new RuntimeException("Mismatched parentheses");
                }
                operatorStack.pop(); // Pop the '('
                // If top is a function, pop it too (e.g., sin( ))
                if (!operatorStack.isEmpty() && isFunction(operatorStack.peek())) {
                    outputQueue.add(operatorStack.pop());
                }
            }
        }

        // Pop remaining operators
        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek().equals("(") || operatorStack.peek().equals(")")) {
                throw new RuntimeException("Mismatched parentheses");
            }
            outputQueue.add(operatorStack.pop());
        }
        System.out.println("Postfix: " + outputQueue); // Debugging
        return outputQueue;
    }

    // --- Postfix Evaluation ---
    private double evaluatePostfix(List<String> postfix) {
        Stack<Double> valueStack = new Stack<>();

        for (String token : postfix) {
            if (isNumeric(token)) {
                valueStack.push(Double.parseDouble(token));
            } else if (isFunction(token) || token.equals("neg") || token.equals("!") || token.equals("%")) { // Unary operators/functions
                if (valueStack.isEmpty()) throw new RuntimeException("Invalid expression (unary op needs operand)");
                double operand = valueStack.pop();
                valueStack.push(applyUnaryOp(token, operand));
            } else if (isOperator(token)) { // Binary operators
                if (valueStack.size() < 2) throw new RuntimeException("Invalid expression (binary op needs two operands)");
                double rightOperand = valueStack.pop();
                double leftOperand = valueStack.pop();
                valueStack.push(applyBinaryOp(token, leftOperand, rightOperand));
            } else {
                throw new RuntimeException("Unknown token in postfix evaluation: " + token);
            }
        }

        if (valueStack.size() != 1) {
            // This can happen with input like "5 5" - not a valid expr ending state
            System.err.println("Invalid final value stack state: " + valueStack);
            throw new RuntimeException("Invalid Expression Format");
        }

        return valueStack.pop();
    }


    // --- Helper Methods ---

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") ||
                token.equals("/") || token.equals("^"); // Basic binary operators
        // Factorial (!) and Percent (%) are handled as unary postfix
        // Unary negate is handled as "neg"
    }

    private boolean isBinaryOperator(String token) { // Helper specifically for input spacing
        return token.equals("+") || token.equals("-") || token.equals("*") ||
                token.equals("/") || token.equals("^");
    }

    private boolean isFunction(String token) {
        return token.equals("sin") || token.equals("cos") || token.equals("tan") ||
                token.equals("log") || token.equals("ln") || token.equals("sqrt");
        // Factorial, Percent, Negate are treated separately
    }

    private int getPrecedence(String operator) {
        switch (operator) {
            case "neg": return 4; // Unary negate highest
            case "^": return 3; // Power
            case "*":
            case "/": return 2; // Multiplication/Division
            case "+":
            case "-": return 1; // Addition/Subtraction
            case "sin":
            case "cos":
            case "tan":
            case "log":
            case "ln":
            case "sqrt":
            case "!":
            case "%": return 4; // Functions and postfix unary also high effective precedence

            default: return 0; // Parentheses, numbers
        }
    }

    private double applyUnaryOp(String op, double operand) {
        switch (op) {
            case "sin": return Math.sin(Math.toRadians(operand)); // Assume degrees input
            case "cos": return Math.cos(Math.toRadians(operand)); // Assume degrees input
            case "tan":
                // Handle tan(90), tan(270) etc. -> undefined
                double degrees = operand % 360;
                if (degrees == 90.0 || degrees == 270.0 || degrees == -90.0 || degrees == -270.0) {
                    throw new ArithmeticException("Tan is undefined for multiples of 90 degrees");
                }
                return Math.tan(Math.toRadians(operand));
            case "log": // Base 10 logarithm
                if (operand <= 0) throw new ArithmeticException("Logarithm requires positive argument");
                return Math.log10(operand);
            case "ln": // Natural logarithm (base e)
                if (operand <= 0) throw new ArithmeticException("Natural log requires positive argument");
                return Math.log(operand);
            case "sqrt": // Square root
                if (operand < 0) throw new ArithmeticException("Square root requires non-negative argument");
                return Math.sqrt(operand);
            case "neg": // Unary negate
                return -operand;
            case "!": // Factorial
                if (operand < 0 || operand != Math.floor(operand)) { // Check for negative or non-integer
                    throw new ArithmeticException("Factorial requires non-negative integer");
                }
                if (operand > 20) { // Avoid overflow with large factorials for double
                    throw new ArithmeticException("Factorial input too large (overflow risk)");
                }
                return factorial((int) operand);
            case "%": // Percent (treat as divide by 100)
                return operand / 100.0;
            default: throw new IllegalArgumentException("Unknown unary operator: " + op);
        }
    }

    private double applyBinaryOp(String op, double left, double right) {
        switch (op) {
            case "+": return left + right;
            case "-": return left - right;
            case "*": return left * right;
            case "/":
                if (right == 0) throw new ArithmeticException("Division by zero");
                return left / right;
            case "^": return Math.pow(left, right);
            default: throw new IllegalArgumentException("Unknown binary operator: " + op);
        }
    }

    private double factorial(int n) {
        if (n == 0 || n == 1) return 1;
        double result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}