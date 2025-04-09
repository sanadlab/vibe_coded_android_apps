package com.sanad.gpt4o.gptscientificcalculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText display;
    private double val1 = Double.NaN;
    private double val2;
    private char CURRENT_OP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);
        display.setShowSoftInputOnFocus(false);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String buttonText = button.getText().toString();
                String data = display.getText().toString();

                switch (buttonText) {
                    case "C":
                        display.setText("");
                        val1 = Double.NaN;
                        CURRENT_OP = '\0';
                        break;
                    case "=":
                        calculate();
                        break;
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                    case "^":
                        if (!Double.isNaN(val1)) {
                            CURRENT_OP = buttonText.charAt(0);
                            display.setText(data + buttonText);
                        }
                        break;
                    case "√":
                        if (!data.isEmpty()) {
                            val1 = Math.sqrt(Double.parseDouble(data));
                            display.setText(String.valueOf(val1));
                        }
                        break;
                    default:
                        display.setText(data + buttonText);
                        break;
                }
            }
        };

        findViewById(R.id.btn0).setOnClickListener(listener);
        findViewById(R.id.btn1).setOnClickListener(listener);
        findViewById(R.id.btn2).setOnClickListener(listener);
        findViewById(R.id.btn3).setOnClickListener(listener);
        findViewById(R.id.btn4).setOnClickListener(listener);
        findViewById(R.id.btn5).setOnClickListener(listener);
        findViewById(R.id.btn6).setOnClickListener(listener);
        findViewById(R.id.btn7).setOnClickListener(listener);
        findViewById(R.id.btn8).setOnClickListener(listener);
        findViewById(R.id.btn9).setOnClickListener(listener);
        findViewById(R.id.btnAdd).setOnClickListener(listener);
        findViewById(R.id.btnSubtract).setOnClickListener(listener);
        findViewById(R.id.btnMultiply).setOnClickListener(listener);
        findViewById(R.id.btnDivide).setOnClickListener(listener);
        findViewById(R.id.btnSqrt).setOnClickListener(listener);
        findViewById(R.id.btnPower).setOnClickListener(listener);
        findViewById(R.id.btnDecimal).setOnClickListener(listener);
        findViewById(R.id.btnClear).setOnClickListener(listener);
        findViewById(R.id.btnEqual).setOnClickListener(listener);
    }

    private void calculate() {
        if (!Double.isNaN(val1) && !display.getText().toString().isEmpty()) {
            String[] splitData = display.getText().toString().split(String.valueOf(CURRENT_OP));
            if (splitData.length > 1) {
                val2 = Double.parseDouble(splitData[1]);
                switch (CURRENT_OP) {
                    case '+':
                        val1 = val1 + val2;
                        break;
                    case '-':
                        val1 = val1 - val2;
                        break;
                    case 'x':
                        val1 = val1 * val2;
                        break;
                    case '/':
                        val1 = val1 / val2;
                        break;
                    case '^':
                        val1 = Math.pow(val1, val2);
                        break;
                }
                display.setText(String.valueOf(val1));
                CURRENT_OP = '\0';
            }
        } else if (!display.getText().toString().isEmpty()) {
            val1 = Double.parseDouble(display.getText().toString());
        }
    }
}