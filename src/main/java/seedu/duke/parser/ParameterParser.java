package seedu.duke.parser;

import seedu.duke.command.Command;
import seedu.duke.command.ListCommand;
import seedu.duke.data.transaction.Expense;
import seedu.duke.data.transaction.Income;
import seedu.duke.exception.InputDuplicateTagException;
import seedu.duke.exception.InputMissingTagException;
import seedu.duke.exception.InputUnsupportedTagException;
import seedu.duke.exception.MoolahException;
import seedu.duke.exception.EmptyParameterException;
import seedu.duke.exception.UnknownHelpOptionException;
import seedu.duke.exception.InputTransactionInvalidDateException;
import seedu.duke.exception.AddTransactionInvalidAmountException;
import seedu.duke.exception.InputTransactionInvalidCategoryException;
import seedu.duke.exception.EntryNumberNotNumericException;
import seedu.duke.exception.InputTransactionUnknownTypeException;
import seedu.duke.exception.ListStatisticsInvalidStatsTypeException;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static seedu.duke.command.CommandTag.COMMAND_TAG_TRANSACTION_TYPE;
import static seedu.duke.command.CommandTag.COMMAND_TAG_TRANSACTION_DATE;
import static seedu.duke.command.CommandTag.COMMAND_TAG_TRANSACTION_CATEGORY;
import static seedu.duke.command.CommandTag.COMMAND_TAG_TRANSACTION_AMOUNT;
import static seedu.duke.command.CommandTag.COMMAND_TAG_TRANSACTION_DESCRIPTION;
import static seedu.duke.command.CommandTag.COMMAND_TAG_LIST_ENTRY_NUMBER;
import static seedu.duke.command.CommandTag.COMMAND_TAG_STATISTICS_TYPE;
import static seedu.duke.command.CommandTag.COMMAND_TAG_HELP_OPTION;

import static seedu.duke.common.DateFormats.DATE_INPUT_PATTERN;

/**
 * Parses the parameter portion of the user input and set the parameters into the Command object
 *
 * <p>The ParameterParser will check that the parameter input portion contains only the supported tags,
 * for each of the supported tag, parses the parameter into the valid form required by the Command object
 * and to store the parsed value inside the command object.
 */
public class ParameterParser {
    private static final String EMPTY_STRING = "";
    private static final String DELIMITER = " ";
    private static final int SPLIT_POSITION = 2;
    private static final String CLASS_TYPE_EXPENSE = "seedu.duke.data.transaction.Expense";
    private static final String CLASS_TYPE_INCOME = "seedu.duke.data.transaction.Income";

    /**
     * To parse the parameters input into proper parameters of the command object.
     *
     * <p>The parameters will go through the following checks during the parsing:
     * 1. Check that the user input contains all mandatory tags of the command
     * 2. Check that the user input does not contain tags not supported by the command
     * 3. Check that the user input does not contain a same tag more than once
     * 4. Check that the user input does not contain a tag without parameter
     * 5. For each parameter, check that the format of the parameter is correct
     *
     * @param command A command object created based on the command word given by user.
     * @throws MoolahException Any command input exceptions captured by Moolah Manager.
     */
    public static void parse(Command command, String parametersInput) throws MoolahException {
        assert command != null;
        String[] splits = parametersInput.split(DELIMITER);

        // Might throw InputMissingTagException
        checkMandatoryTagsExist(command, splits);

        /*
        An empty parameterInput suggests that no tag is provided by user. Since it passes the check
        for mandatory check, it also means that the command does not have a mandatory tag. Therefore,
        there is no more need to further check and set the parameters for empty parameters input.
        */
        if (!parametersInput.isEmpty()) {

            // Might throw InputUnsupportedTagException
            checkUnsupportedTagsNotExist(command, splits);
            // Might throw InputDuplicateTagException
            checkDuplicateTagsNotExist(splits);
            // Might throw InputMissingParameterException
            checkParameterNotEmpty(splits);

            // The parameters input contains only the supported tags.
            // For each tag, check that the parameter is correct and set it inside the command.
            setCommand(command, splits);
        }


    }

    /**
     * Checks if all the mandatory tags exists in the split user inputs.
     *
     * @param command A command object created based on the command word given by user.
     * @param splits  The user input after the command word, split into a list for every space found.
     * @throws InputMissingTagException Missing mandatory tag exception.
     */
    private static void checkMandatoryTagsExist(Command command, String[] splits) throws InputMissingTagException {
        String[] tags = command.getMandatoryTags();
        for (String tag : tags) {
            boolean found = findMatchingTagAmongInputs(tag, splits);
            if (!found) {
                throw new InputMissingTagException();
            }
        }
    }

    /**
     * Checks if the split user inputs contains any unsupported tag.
     *
     * @param command A command object created based on the command word given by user.
     * @param splits  The user input after the command word, split into a list for every space found.
     * @throws InputUnsupportedTagException Extra tag exception.
     */
    private static void checkUnsupportedTagsNotExist(Command command, String[] splits)
            throws InputUnsupportedTagException {
        String[] mandatoryTags = command.getMandatoryTags();
        String[] optionalTags = command.getOptionalTags();

        for (String split : splits) {
            if (split.length() < 2) {
                // None of the tag is shorter than two characters
                throw new InputUnsupportedTagException();
            }
            boolean hasFoundAmongMandatoryTag = findIfParameterTagAmongTags(split, mandatoryTags);
            boolean hasFoundAmongOptionalTag = findIfParameterTagAmongTags(split, optionalTags);
            if (hasFoundAmongMandatoryTag || hasFoundAmongOptionalTag) {
                continue;
            }

            // Found a tag entered by the user but does not exist in the supported tag for the command
            throw new InputUnsupportedTagException();
        }
    }

    /**
     * Checks if the split user inputs contains a tag multiple times.
     *
     * @param splits The user input after the command word, split into a list for every space found.
     * @throws InputDuplicateTagException Extra tag exception.
     */
    private static void checkDuplicateTagsNotExist(String[] splits) throws InputDuplicateTagException {
        HashMap<String, Integer> tagOccurenceMap = new HashMap<>();
        for (String split : splits) {
            String tag = split.substring(0, SPLIT_POSITION);

            // The duplicated tag can be found in the hash map
            if (tagOccurenceMap.containsKey(tag)) {
                throw new InputDuplicateTagException();
            }
            tagOccurenceMap.put(tag, 1);
        }
    }

    /**
     * Checks if there are missing parameter within the user input.
     * If the split.length() is <= 2, it means that only the tag exists , and there is no parameter after the tag.
     *
     * @param splits The user input after the command word, split into a list for every space found.
     * @throws EmptyParameterException Extra tag exception.
     * @author chinhan99
     */
    private static void checkParameterNotEmpty(String[] splits) throws EmptyParameterException {
        for (String split : splits) {
            if (split.length() <= 2) {
                throw new EmptyParameterException();
            }
        }
    }

    /**
     * Returns a boolean value on whether a tag can be found among the split user inputs.
     *
     * @param tag    A specific tag used to locate the command parameter.
     * @param splits The user input after the command word, split into a list for every space found.
     * @return Whether the tag is found within the split inputs.
     */
    private static boolean findMatchingTagAmongInputs(String tag, String[] splits) {
        boolean hasFound = false;
        for (String split : splits) {
            hasFound = split.startsWith(tag);
            if (hasFound) {
                break;
            }
        }
        return hasFound;
    }

    /**
     * Returns a boolean value on whether a tag can be found among the split user inputs.
     *
     * @param parameter A user parameter input entered after the command word.
     * @param tags      An array of tags.
     * @return Whether the tag is found within the split inputs.
     */
    private static boolean findIfParameterTagAmongTags(String parameter, String[] tags) {
        boolean hasFound = false;

        String parameterTag = parameter.substring(0, SPLIT_POSITION);
        for (String tag : tags) {
            hasFound = tag.equals(parameterTag);
            if (hasFound) {
                break;
            }
        }
        return hasFound;
    }

    /**
     * For each split parameters, split it into tag and parameter, then check and set the parameters into the Command.
     *
     * @param command A command object created based on the command word given by user.
     * @param splits  The user input after the command word, split into a list for every space found.
     * @throws MoolahException Any command input exceptions captured by Moolah Manager.
     */
    private static void setCommand(Command command, String[] splits) throws MoolahException {
        for (String split : splits) {
            String tag = split.substring(0, SPLIT_POSITION);
            String parameter = split.substring(SPLIT_POSITION);
            setParameter(command, tag, parameter);
        }
    }

    private static void setParameter(Command command, String tag, String parameter) throws MoolahException {
        switch (tag) {
        case COMMAND_TAG_TRANSACTION_TYPE:
            // TODO: To standardise the format for transaction type for add and list
            if (command instanceof ListCommand) {
                command.setType(parseTypeTagForListing(parameter));
            } else {
                command.setType(parseTypeTagForAdding(parameter));
            }
            break;
        case COMMAND_TAG_TRANSACTION_CATEGORY:
            command.setCategory(parseCategoryTag(parameter));
            break;
        case COMMAND_TAG_TRANSACTION_AMOUNT:
            command.setAmount(parseAmountTag(parameter));
            break;
        case COMMAND_TAG_TRANSACTION_DATE:
            command.setDate(parseDateTag(parameter));
            break;
        case COMMAND_TAG_TRANSACTION_DESCRIPTION:
            command.setDescription(parameter);
            break;
        case COMMAND_TAG_LIST_ENTRY_NUMBER:
            command.setEntryNumber(parseEntryTag(parameter));
            break;
        case COMMAND_TAG_HELP_OPTION:
            command.setIsDetailedOption(parseHelpOptionTag(parameter));
            break;
        case COMMAND_TAG_STATISTICS_TYPE:
            command.setStatsType(parseStatsTypeTag(parameter));
            break;
        default:
            throw new InputMissingTagException();
        }
    }

    /**
     * Parses the user parameter input for the description and returns it.
     *
     * @param parameter The user input after the user tag.
     * @return The class type if no exceptions are thrown.
     * @throws InputTransactionUnknownTypeException Invalid type format exception.
     * @author chydarren
     */
    public static String parseTypeTagForListing(String parameter) throws InputTransactionUnknownTypeException {
        switch (parameter) {
        case "expense":
            return CLASS_TYPE_EXPENSE;
        case "income":
            return CLASS_TYPE_INCOME;
        default:
            throw new InputTransactionUnknownTypeException();
        }
    }

    /**
     * Check if the type parameter is a valid transaction type and returns the parameter if it is valid.
     *
     * @param parameter The user input after the user tag.
     * @return The user input after the user tag.
     * @throws InputTransactionUnknownTypeException Invalid type format exception.
     * @author wcwy
     */
    public static String parseTypeTagForAdding(String parameter) throws InputTransactionUnknownTypeException {
        boolean isExpense = parameter.equals(Expense.TRANSACTION_NAME);
        boolean isIncome = parameter.equals(Income.TRANSACTION_NAME);

        if (!isExpense && !isIncome) {
            throw new InputTransactionUnknownTypeException();
        }

        return parameter;
    }

    /**
     * Parses the user parameter input for the description and returns it.
     *
     * @param parameter The user input after the user tag.
     * @return The category parameter if no exceptions are thrown.
     * @throws InputTransactionInvalidCategoryException Invalid category format exception.
     * @author chinhan99
     */
    public static String parseCategoryTag(String parameter) throws InputTransactionInvalidCategoryException {
        Pattern specialSymbols = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
        Matcher hasSpecialSymbols = specialSymbols.matcher(parameter);
        if (containNumeric(parameter) || hasSpecialSymbols.find()) {
            throw new InputTransactionInvalidCategoryException();
        }
        return parameter;
    }

    /**
     * Parses the user parameter input for the amount and returns it.
     *
     * @param parameter The user input after the user tag.
     * @return The amount integer if no exceptions are thrown.
     * @throws AddTransactionInvalidAmountException Invalid amount format exception.
     * @author chinhan99
     */
    private static int parseAmountTag(String parameter) throws AddTransactionInvalidAmountException {
        Pattern specialSymbols = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
        Matcher hasSpecialSymbols = specialSymbols.matcher(parameter);
        try {
            if (containAlphabet(parameter) || hasSpecialSymbols.find()) {
                throw new AddTransactionInvalidAmountException();
            }
            int amount = Integer.parseInt(parameter);
            if (amount < 0 || amount > 10000000) {
                throw new AddTransactionInvalidAmountException();
            }
            return amount;

        } catch (NumberFormatException e) {
            throw new AddTransactionInvalidAmountException();
        }
    }

    /**
     * Parses the user parameter input for date into a LocalDate object and returns it.
     *
     * @param parameter The user input after the user tag.
     * @return The LocalDate object parsed from user input given.
     * @throws InputTransactionInvalidDateException Invalid date format exception.
     * @author wcwy
     */
    public static LocalDate parseDateTag(String parameter) throws InputTransactionInvalidDateException {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_INPUT_PATTERN.toString());
            LocalDate date = LocalDate.parse(parameter, formatter);
            return date;
        } catch (DateTimeParseException exception) {
            throw new InputTransactionInvalidDateException();
        }
    }

    /**
     * Parses the user parameter input for entry number into an integer and returns it.
     *
     * @param parameter The user input after the user tag.
     * @return The valid integer for list index parsed from user input given.
     * @throws MoolahException Either invalid index exception or amount not numeric exception.
     * @author brian-vb
     */
    public static int parseEntryTag(String parameter) throws MoolahException {
        int index;
        try {
            index = Integer.parseInt(parameter);
        } catch (NumberFormatException e) {
            throw new EntryNumberNotNumericException();
        }

        return index;
    }

    /**
     * Return a boolean value indicating if the option selected by user is "detailed".
     *
     * @param parameter The user input after the user tag.
     * @return A boolean value indicating if the option selected by user is "detailed"
     * @throws UnknownHelpOptionException An unknown help option exception.
     * @author wcwy
     */
    public static boolean parseHelpOptionTag(String parameter) throws UnknownHelpOptionException {
        boolean isValidHelpOption = parameter.equals("detailed");
        if (isValidHelpOption) {
            return true;
        } else {
            throw new UnknownHelpOptionException();
        }
    }

    /**
     * Check if the type parameter is a valid statistic type and returns the parameter if it is valid.
     *
     * @param parameter The user input after the user tag.
     * @return The statistic type.
     * @throws ListStatisticsInvalidStatsTypeException Invalid statistic type exception.
     * @author brian-vb
     */
    public static String parseStatsTypeTag(String parameter) throws ListStatisticsInvalidStatsTypeException {
        String statsType = EMPTY_STRING;
        switch (parameter) {
        case "categories":
            statsType = "categories";
            break;
        default:
            throw new ListStatisticsInvalidStatsTypeException();
        }
        return statsType;
    }


    /**
     * Checks if the parameter contains numeric characters.
     *
     * @param parameter The user input after the user tag.
     * @return A boolean value indicating whether there are numeric characters within the parameter.
     * @author chinhan99
     */
    public static boolean containNumeric(String parameter) {
        char[] characters = parameter.toCharArray();
        for (char character : characters) {
            if (Character.isDigit(character)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the parameter contains alphabetical characters.
     *
     * @param parameter The user input after the user tag.
     * @return true if there are alphabetical characters within the parameter.
     * @author chinhan99
     */
    public static boolean containAlphabet(String parameter) {
        char[] characters = parameter.toCharArray();
        for (char character : characters) {
            if (Character.isAlphabetic(character)) {
                return true;
            }
        }
        return false;
    }
}
