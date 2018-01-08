package ru.mail.polis.netty.services.server_side_processing;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.*;

class Performer {
    private static final String DELETE = "del";
    private static final String DELETEC = "delc";
    private static final String MOVE = "mov";
    private static final String COPY = "cop";
    private static final String FIND = "fin";
    private static final String RENAME = "ren";
    private static final String ADD = "add";
    private static final String SIZE = "siz";
    private static final String READ = "rea";
    private static final String WRITE = "wrt";
    private static final String EQUALS = "eq";
    private static final String PLUS = "pls";
    private static final String JUMP = "jmp";
    private static final String ARRAY = "arr";
    private static final String PUT = "put";
    private static final String REMOVE = "rem";
    private static final String GET = "get";
    private static final String OUT = "out";
    private static final String SIZE_OF_ARRAY = "soa";

    private static final String EAX = "eax";
    private static final String EBX = "ebx";
    private static final String ECX = "ecx";
    private static final String EDX = "edx";
    private static final String RES = "res";

    private Integer INSTRUCTION_PTR;

    private LinkedHashMap<Integer, String[]> commands = new LinkedHashMap<>();
    private SyntaxValidator validator;
    private List<String> lines;
    private Object eax, ebx, ecx, edx, res;
    private HashMap<String, LinkedList<Object>> arrays = new HashMap<>();

    Performer(@NotNull List<String> lines) {
        this.lines = lines;
    }

    private boolean validate() {
        validator = new SyntaxValidator(lines);
        try {
            return validator.validate();
        } catch (SyntaxValidator.SyntaxErrorException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    private void prepare() {
        if (validate()) {
            for (String line : lines) {
                String[] tokens = line.split("[ ,]+");
                commands.put(Integer.parseInt(tokens[0]), tokens);
            }
        }
    }

    void perform() {
        prepare();

        Object[] it = commands.keySet().toArray();
        int i = 0;
        while (i < it.length) {
            INSTRUCTION_PTR = (Integer) it[i];
            String[] params = commands.get(INSTRUCTION_PTR);

            if (params[1].equals(DELETE)) {
                delete(params);
            }

            if (params[1].equals(MOVE)) {
                move(params);
            }

            if (params[1].equals(COPY)) {
                copy(params);
            }

            if (params[1].equals(FIND)) {
                find(params);
            }

            if (params[1].equals(RENAME)) {
                rename(params);
            }

            if (params[1].equals(EQUALS)) {
                condition(params);
            }

            if (params[1].equals(SIZE)) {
                size(params);
            }

            if (params[1].equals(ADD)) {
                add(params);
            }

            if (params[1].equals(READ)) {
                read(params);
            }

            if (params[1].equals(WRITE)) {
                write(params);
            }

            if (params[1].equals(ARRAY)) {
                array(params);
            }

            if (params[1].equals(GET)) {
                try {
                    get(params);
                } catch (SyntaxValidator.SyntaxErrorException e) {
                    System.err.println(e.getMessage());
                }
            }

            if (params[1].equals(PUT)) {
                put(params);
            }

            if (params[1].equals(REMOVE)) {
                remove(params);
            }

            if (params[1].equals(SIZE_OF_ARRAY)) {
                sizeOfArray(params);
            }

            if (params[1].equals(DELETEC)) {
                deletec(params);
            }

            if (params[1].equals(PLUS)) {
                plus(params);
            }

            if (params[1].equals(OUT)) {
                out(params);
            }

            if (params[1].equals(JUMP)) {
                int sc = INSTRUCTION_PTR;
                jump(params);

                if (sc != INSTRUCTION_PTR) { // был прыжок
                    for (int k = 0; k < it.length; k++) {
                        if (it[k].equals(INSTRUCTION_PTR)) {
                            i = k - 1;
                            break;
                        }
                    }
                }
            }
            i++;
        }
    }

    private void out(String[] arguments) {
        String token = arguments[2];

        if (isRegister(token)) {
            System.out.println("Register " + token + " contains: " + getValueFromRegister(token));
        } else {
            System.out.println("Array " + token + " contains: " + Arrays.toString(arrays.get(token).toArray()));
        }
    }

    private void delete(String[] arguments) {
        String register = arguments[2];
        Object value;

        if (isRegister(arguments[3])) {
            value = getValueFromRegister(arguments[3]);
        } else {
            value = arguments[3];
        }

        Path path = Paths.get((String) value);
        try {
            Files.delete(path);
            setValueToRegister(register, 1);
        } catch (IOException e) {
            setValueToRegister(register, 0);
        }
    }

    private void deletec(String[] arguments) {
        String register = arguments[2];
        Object value;
        Integer address = Integer.valueOf(arguments[4]);

        if (isRegister(arguments[3])) {
            value = getValueFromRegister(arguments[3]);
        } else {
            value = arguments[3];
        }

        if (condition(commands.get(address))) {
            Path path = Paths.get((String) value);
            try {
                Files.delete(path);
                setValueToRegister(register, 1);
            } catch (IOException e) {
                setValueToRegister(register, 0);
            }
        }
    }

    private void array(String[] arguments) {
        String arrayName = arguments[2];
        LinkedList<Object> list = new LinkedList<>();

        for (int i = 0; i < arguments.length - 3; i++) {
            list.add(arguments[i + 3]);
        }

        arrays.put(arrayName, list);
    }

    private void sizeOfArray(String[] arguments) {
        String register = arguments[2];
        String arrayName = arguments[3];
        LinkedList<Object> arr = arrays.get(arrayName);

        if (arr != null) {
            setValueToRegister(register, arr.size());
        } else {
            System.err.println("Error: array " + arrayName + " doesn't exist.");
        }
    }

    private void put(String[] arguments) {
        String arrayName = arguments[2];
        Object value = arguments[3];

        LinkedList<Object> list = arrays.get(arrayName);
        if (list != null) {
            list.add(value);
        }
    }

    private void remove(String[] arguments) {
        String arrayName = arguments[2];
        Object value = arguments[3];

        LinkedList<Object> list = arrays.get(arrayName);
        if (list != null) {
            list.remove(value);
        }
    }

    private void get(String[] arguments) throws ArrayIndexOutOfBoundsException, SyntaxValidator.SyntaxErrorException {
        String register = arguments[2];
        String arrayName = arguments[3];

        int index = 0;
        if (isRegister(arguments[4])) {
            if (getValueFromRegister(arguments[4]).getClass().equals(Integer.class)) {
                index = (int) getValueFromRegister(arguments[4]);
            } else if (getValueFromRegister(arguments[4]).getClass().equals(String.class)) {
                try {
                    index = Integer.parseInt((String) getValueFromRegister(arguments[4]));
                } catch (NumberFormatException e) {
                    throw new SyntaxValidator.SyntaxErrorException("The get method doesn't contains string variable " +
                            "in register " + arguments[4] + ".");
                }
            }
        } else {
            index = Integer.parseInt(arguments[4]);
        }

        LinkedList<Object> list = arrays.get(arrayName);
        if (list != null) {
            if (list.size() <= index) {
                throw new ArrayIndexOutOfBoundsException("Index out of bounds. Size: " + list.size() + ", index: " +
                        index + ".");
            } else {
                setValueToRegister(register, arrays.get(arrayName).get(index));
            }
        }
    }

    private void jump(String[] arguments) {
        Integer address = Integer.valueOf(arguments[2]);
        Integer conditionAddress = Integer.valueOf(arguments[3]);

        if (condition(commands.get(conditionAddress))) {
            INSTRUCTION_PTR = address;
        }
    }

    private void plus(String[] arguments) {
        String register = arguments[2];
        Integer value, oldValue = null;

        if (getValueFromRegister(register).getClass().equals(Integer.class)) {
            oldValue = (Integer) getValueFromRegister(register);
        }

        if (getValueFromRegister(register).getClass().equals(String.class)) {
            oldValue = Integer.parseInt((String) getValueFromRegister(register));
        }

        if (isRegister(arguments[3])) {
            value = (Integer) getValueFromRegister(arguments[3]);
        } else {
            value = Integer.valueOf(arguments[3]);
        }

        setValueToRegister(register, oldValue + value);
    }

    private void move(String[] arguments) {
        String register = arguments[2];
        Object value1, value2;

        if (isRegister(arguments[3])) {
            value1 = getValueFromRegister(arguments[3]);
        } else {
            value1 = arguments[3];
        }

        if (isRegister(arguments[4])) {
            value2 = getValueFromRegister(arguments[4]);
        } else {
            value2 = arguments[4];
        }

        Path source = Paths.get((String) value1);
        Path dest = Paths.get((String) value2);
        try {
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
            setValueToRegister(register, 1);
        } catch (IOException e) {
            setValueToRegister(register, 0);
        }
    }

    private void copy(String[] arguments) {
        String register = arguments[2];
        Object value1, value2;

        if (isRegister(arguments[3])) {
            value1 = getValueFromRegister(arguments[3]);
        } else {
            value1 = arguments[3];
        }

        if (isRegister(arguments[4])) {
            value2 = getValueFromRegister(arguments[4]);
        } else {
            value2 = arguments[4];
        }

        Path source = Paths.get((String) value1);
        Path dest = Paths.get((String) value2);
        try {
            Files.copy(source, dest);
            setValueToRegister(register, 1);
        } catch (IOException e) {
            setValueToRegister(register, 0);
        }
    }

    private void find(String[] arguments) {
        String register = arguments[2];
        Object value;

        if (isRegister(arguments[3])) {
            value = getValueFromRegister(arguments[3]);
        } else {
            value = arguments[3];
        }

        File file = new File((String) value);
        if (file.exists() && !file.isDirectory()) {
            setValueToRegister(register, 1);
        } else {
            setValueToRegister(register, 0);
        }
    }

    private void rename(String[] arguments) {
        String register = arguments[2];
        String source;
        String name;

        if (isRegister(arguments[3])) {
            source = (String) getValueFromRegister(arguments[3]);
        } else {
            source = arguments[3];
        }

        if (isRegister(arguments[4])) {
            name = (String) getValueFromRegister(arguments[4]);
        } else {
            name = arguments[4];
        }

        String[] tokens = source.split(File.separator);
        source = source.replaceAll(tokens[tokens.length - 1], name);

        File file = new File(source);
        if (file.exists() && !file.isDirectory()) {
            if (file.renameTo(new File(source))) {
                setValueToRegister(register, 1);
            } else {
                setValueToRegister(register, 0);
            }
        }
    }

    private void add(String[] arguments) {
        String register = arguments[2];
        Object value;

        if (isRegister(arguments[3])) {
            value = getValueFromRegister(arguments[3]);
        } else {
            value = arguments[3];
        }

        setValueToRegister(register, value);
    }

    private void size(String[] arguments) {
        String register = arguments[2];
        Object value;

        if (isRegister(arguments[3])) {
            value = getValueFromRegister(arguments[3]);
        } else {
            value = arguments[3];
        }

        Path path = Paths.get((String) value);
        try {
            Object size = Files.getAttribute(path, "size");
            setValueToRegister(register, size);
        } catch (IOException e) {
            setValueToRegister(register, -1);
        }
    }

    private void write(String[] arguments) {
        String register = arguments[2];
        String parameter = arguments[5];
        Object path, str;

        if (isRegister(arguments[3])) {
            path = getValueFromRegister(arguments[3]);
        } else {
            path = arguments[3];
        }

        if (isRegister(arguments[4])) {
            str = getValueFromRegister(arguments[4]);
        } else {
            str = arguments[4];
        }

        File file = new File((String) path);
        if (file.exists() && !file.isDirectory()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, parameter.equals("1")))) {
                writer.write((String) str);
                writer.flush();
                setValueToRegister(register, 1);
            } catch (IOException e) {
                setValueToRegister(register, 0);
            }
        } else {
            setValueToRegister(register, 0);
        }
    }

    private void read(String[] arguments) {
        Object value;
        StringBuilder builder = new StringBuilder();

        if (isRegister(arguments[2])) {
            value = getValueFromRegister(arguments[2]);
        } else {
            value = arguments[2];
        }

        File file = new File((String) value);
        if (file.exists() && !file.isDirectory()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            } catch (IOException e) {
                System.err.println("Cannot read file " + value + ".");
            }

            System.out.println("File contents: " + value);
            System.out.println("------------------------");
            System.out.print(builder.toString());
            System.out.println("------------------------");
        } else {
            System.err.println("File " + value  +" not found.");
        }
    }

    private boolean condition(String[] arguments) {
        LinkedList<Object> parsedCondition = new LinkedList<>();
        Object op1, op2;

        try {
            performCondition(arguments, parsedCondition);
        } catch (SyntaxValidator.SyntaxErrorException e) {
            System.err.println(e.getMessage());
        }

        // дерево условий выполнено, каждый отдельный операнд посчитан, осталось посчитать условие
        if (parsedCondition.size() == 1) {
            return (boolean) parsedCondition.getFirst();
        }

        int i = 0;
        while (i < parsedCondition.size() - 1) {
            if (parsedCondition.get(i + 1).getClass().equals(String.class) && parsedCondition.get(i + 1).equals("and")) {
                op1 = parsedCondition.get(i);
                parsedCondition.remove(i);
                parsedCondition.remove(i);
                op2 = parsedCondition.get(i);
                parsedCondition.remove(i);

                if (((boolean) op1) & ((boolean) op2)) {
                    parsedCondition.add(i, true);
                } else {
                    parsedCondition.add(i, false);
                }
                i--;
            }
            i++;
        }


        if (parsedCondition.size() == 1) {
            return (boolean) parsedCondition.getFirst();
        }

        i = 0;
        while (i < parsedCondition.size() - 1) {
            if (parsedCondition.get(i + 1).getClass().equals(String.class) && parsedCondition.get(i + 1).equals("or")) {
                op1 = parsedCondition.get(i);
                parsedCondition.remove(i);
                parsedCondition.remove(i);
                op2 = parsedCondition.get(i);
                parsedCondition.remove(i);

                if (((boolean) op1) | ((boolean) op2)) {
                    parsedCondition.add(i, true);
                } else {
                    parsedCondition.add(i, false);
                }
                i--;
            }
            i++;
        }

        return (boolean) parsedCondition.getFirst();
    }

    private void performCondition(String[] arguments, LinkedList<Object> parsedCondition)
            throws SyntaxValidator.SyntaxErrorException {

        LinkedList<Character> split = new LinkedList<>();
        ArrayList<Integer> indexes = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        String condition = arguments[3];
        boolean res;

        for (int i = 0; i < condition.length(); i++) {
            split.add(condition.charAt(i));
        }

        int lock = 0;
        for (int i = 0; i < split.size() - 1; i++) {
            Character ch1 = split.get(i);
            Character ch2 = split.get(i + 1);

            if (ch1.equals('\"')) {
                lock++;
            }

            if (lock % 2 == 0) {
                if (!((Character.isDigit(ch1) || Character.isLetter(ch1)) &&
                        (Character.isDigit(ch2) || Character.isLetter(ch2)))) {
                    if (!(ch1.equals('=') & ch2.equals('=') || ch1.equals('<') & ch2.equals('=')
                            || ch1.equals('>') & ch2.equals('='))) {
                        indexes.add(i + 1);
                    }
                }
            }
        }

        int cnt = 0;
        for (int i = 0; i < indexes.size(); i++) {
            split.add(indexes.get(i) + cnt, ' ');
            cnt++;
        }

        for (int i = 0; i < split.size(); i++) {
            builder.append(split.get(i));
        }

        String[] tokens = builder.toString().split("[ ]+");
        Object op1, op2;
        String operand = tokens[2];

        if (isRegister(tokens[1])) {
            op1 = getValueFromRegister(tokens[1]);
        } else {
            op1 = tokens[1];
        }

        if (isRegister(tokens[3])) {
            op2 = getValueFromRegister(tokens[3]);
        } else {
            op2 = tokens[3];
        }

        if (arguments.length == 6) {
            res = performSubCondition(op1, op2, operand);
            if (res) {
                setValueToRegister(arguments[2], 1);
            } else {
                setValueToRegister(arguments[2], 0);
            }

            parsedCondition.addLast(res);
            parsedCondition.addLast(arguments[4]);

            Integer next = Integer.valueOf(arguments[5]);
            performCondition(commands.get(next), parsedCondition);
        }

        if (arguments.length == 4) {
            res = performSubCondition(op1, op2, operand);
            if (res) {
                setValueToRegister(arguments[2], 1);
            } else {
                setValueToRegister(arguments[2], 0);
            }

            parsedCondition.addLast(res);
        }
    }

    private boolean performSubCondition(Object op1,
                                        Object op2,
                                        String operand) throws SyntaxValidator.SyntaxErrorException {
        try {
            if (op1.getClass().equals(String.class)) {
                op1 = Integer.parseInt((String) op1);
            }

            if (op2.getClass().equals(String.class)) {
                op2 = Integer.parseInt((String) op2);
            }
        } catch (ClassCastException e) {
            // do nothing
        }

        if (op1.getClass().equals(op2.getClass()) & (op1.getClass().equals(String.class))) {
            switch (operand) {
                case "<":
                    return ((String) op1).compareTo((String) op2) < 0;
                case ">":
                    return ((String) op1).compareTo((String) op2) > 0;
                case "==":
                    return ((String) op1).compareTo((String) op2) == 0;
                case "<=":
                    return ((String) op1).compareTo((String) op2) <= 0;
                case ">=":
                    return ((String) op1).compareTo((String) op2) >= 0;
            }
        } else if (op1.getClass().equals(op2.getClass()) & (op1.getClass().equals(Integer.class))) {
            switch (operand) {
                case "<":
                    return ((Integer) op1).compareTo((Integer) op2) < 0;
                case ">":
                    return ((Integer) op1).compareTo((Integer) op2) > 0;
                case "==":
                    return ((Integer) op1).compareTo((Integer) op2) == 0;
                case "<=":
                    return ((Integer) op1).compareTo((Integer) op2) <= 0;
                case ">=":
                    return ((Integer) op1).compareTo((Integer) op2) >= 0;
            }
        } else {
            throw new SyntaxValidator.SyntaxErrorException("Incompatible types: " + op1 + " instance of " +
                    op1.getClass().getSimpleName() + ", " + op2 + " instance of " + op2.getClass().getSimpleName());
        }
        return false;
    }

    private boolean isRegister(String value) {
        return value.equals(EAX) || value.equals(EBX) || value.equals(ECX) || value.equals(EDX) || value.equals(RES);
    }

    private Object getValueFromRegister(String register) {
        Object val = null;
        switch (register) {
            case EAX:
                val = eax;
                break;
            case EBX:
                val = ebx;
                break;
            case ECX:
                val = ecx;
                break;
            case EDX:
                val = edx;
                break;
            case RES:
                val = res;
                break;
        }
        return val;
    }

    private void setValueToRegister(String register, Object val) {
        switch (register) {
            case EAX:
                eax = val;
                break;
            case EBX:
                ebx = val;
                break;
            case ECX:
                ecx = val;
                break;
            case EDX:
                edx = val;
                break;
            case RES:
                res = val;
                break;
        }
    }
}
