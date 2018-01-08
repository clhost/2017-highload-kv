package ru.mail.polis.netty.services.server_side_processing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SyntaxValidator {
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

    private List<String> lines;
    private final Set<String> commands =
            new HashSet<>(Arrays.asList(
                    "del", "delc", "mov", "cop",
                    "fin", "ren", "eq", "add",
                    "siz", "rea", "wrt", "arr",
                    "put", "rem", "get", "pls",
                    "jmp", "out", "soa"));

    private final Set<String> registers =
            new HashSet<>(Arrays.asList("eax", "ebx", "ecx", "edx", "res"));

    SyntaxValidator(List<String> lines) {
        this.lines = lines;
    }

    boolean validate() throws SyntaxErrorException {
        for (String line : lines) {
            String[] tokens = line.split("[ ,]+");

            if (tokens.length < 3) {
                throw new SyntaxErrorException("At line " + line + " not enough words.");
            }

            if (!commands.contains(tokens[1])) {
                throw new SyntaxErrorException("At line " + line + " invalid command: " + tokens[1] + ".");
            }

            if (tokens[1].equals(DELETE) || tokens[1].equals(FIND) || tokens[1].equals(SIZE) || tokens[1].equals(ADD)) {
                if (tokens.length != 4) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (!registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " invalid register: " + tokens[2] + ".");
                }
            }

            if (tokens[1].equals(DELETEC)) {
                if (tokens.length != 5) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (!registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " invalid register: " + tokens[2] + ".");
                }

                try {
                    Integer.parseInt(tokens[4]);
                } catch (NumberFormatException e) {
                    throw new SyntaxErrorException("At line " + line + " invalid address: " + tokens[2] + ".");
                }
            }

            if (tokens[1].equals(MOVE) || tokens[1].equals(COPY) || tokens[1].equals(RENAME)) {
                if (tokens.length != 5) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (!registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " invalid register: " + tokens[2] + ".");
                }
            }

            if (tokens[1].equals(WRITE)) {
                if (tokens.length != 6) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (!registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " invalid register: " + tokens[2] + ".");
                }

                try {
                    int parameter = Integer.parseInt(tokens[5]);
                    if (parameter != 0 && parameter != 1) {
                        throw new SyntaxErrorException("At line " + line + " invalid parameter: " + tokens[4] + ". " +
                                "Parameter must be 0 or 1.");
                    }
                } catch (NumberFormatException e) {
                    throw new SyntaxErrorException("At line " + line + " invalid address: " + tokens[2] + ".");
                }
            }

            if (tokens[1].equals(READ) || tokens[1].equals(OUT)) {
                if (tokens.length > 3) {
                    throw new SyntaxErrorException("At line " + line + " too much words.");
                }
            }

            if (tokens[1].equals(JUMP)) {
                if (tokens.length != 4) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                try {
                    Integer.parseInt(tokens[2]);
                } catch (NumberFormatException e) {
                    throw new SyntaxErrorException("At line " + line + " invalid address: " + tokens[2] + ".");
                }

                try {
                    Integer.parseInt(tokens[3]);
                } catch (NumberFormatException e) {
                    throw new SyntaxErrorException("At line " + line + " invalid address: " + tokens[3] + ".");
                }
            }

            if (tokens[1].equals(ARRAY)) {
                if (commands.contains(tokens[2]) || registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " the name of the array shouldn't be a " +
                            "reserved word.");
                }
            }

            if (tokens[1].equals(PUT) || tokens[1].equals(REMOVE)) {
                if (tokens.length != 4) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (commands.contains(tokens[2]) || registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " the name of the array shouldn't be a " +
                            "reserved word.");
                }
            }

            if (tokens[1].equals(GET)) {
                if (tokens.length != 5) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (!registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " invalid register: " + tokens[2] + ".");
                }
            }

            if (tokens[1].equals(EQUALS)) {
                if (tokens.length == 6) {
                    boolean cond;
                    try {
                        cond = Integer.parseInt(tokens[0]) >= Integer.parseInt(tokens[5]);
                    } catch (NumberFormatException e) {
                        throw new SyntaxErrorException("At line " + line + " addresses have invalid number formats.");
                    }
                    if (cond) {
                        throw new SyntaxErrorException("At line " + line + " the address number must be less than " +
                                "next condition address: expected " + tokens[0] + " < " + tokens[5] + ", got " +
                                tokens[0] + " >= " + tokens[5] + ".");
                    }
                }
            }

            if (tokens[1].equals(PLUS) || tokens[1].equals(SIZE_OF_ARRAY)) {
                if (tokens.length != 4) {
                    throw new SyntaxErrorException("At line " + line + " not enough words.");
                }

                if (!registers.contains(tokens[2])) {
                    throw new SyntaxErrorException("At line " + line + " invalid register: " + tokens[2] + ".");
                }
            }
        }
        return true;
    }

    static class SyntaxErrorException extends Exception {
        SyntaxErrorException(String message) {
            super(message);
        }
    }
}