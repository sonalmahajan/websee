package config;

import java.io.File;
import java.util.ArrayList;

public final class Constants
{
	public static final String SAVED_HTML_FILENAME = "savedHTML.html";
	public static final String REPORT_FILENAME = "_report";
	public static final String REPORT_FILE_EXTENSION = "txt";
	public static final String MATCH_FILE = ".*.html";
	public static final String IMAGE_EXTENSION = "png";
	public static final String SCREENSHOT_FILE_EXTENSION = "." + IMAGE_EXTENSION;
	public static final String COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME = "diff.txt";
	public static final String COMPARE_IMAGES_DIFFERENCE_IMAGENAME = "diff" + SCREENSHOT_FILE_EXTENSION;
	public static final String CLUSTERING_DIFFERENCE_PIXELS_IMAGENAME = "diff_clustered" + SCREENSHOT_FILE_EXTENSION;
	public static final String DIFFERENCE_IMAGE_FILTERED_WITH_SPECIAL_REGIONS_IMAGENAME = "diff_filtered" + SCREENSHOT_FILE_EXTENSION;
	public static final String FILE_EXTENSION_REGEX = "\\.";
	public final static String NEW_FILE_NAME = "test.html";
	public final static String NEW_FILES_DIRECTORY = "tests";
	public final static String NEW_FILE_DIRECTORY = "test";
	public final static String ORIGINAL_FILENAME = "oracle.html";
	public final static String ORACLE_IMAGE_FILENAME = "oracle.png";
	public final static String ORACLE_HTML_FILENAME = "oracle.html";
	public final static String REGEX_FOR_GETTING_ID = "\\*\\[@id=['|\"]?(.+[^'\"])['|\"]?\\]";
	public final static String REGEX_FOR_GETTING_INDEX = "\\[(.+)\\]";
	public final static String PREFIX_FOR_RESULT_HTML = "marked_";
	public final static String CHARSET_REGEX = "(?i)\\bcharset=\\s*\"?([^\\s;\"]*)";
	public final static String DEFAULT_CHARSET = "UTF-8";
	public final static String HTML_ELEMENTS_JAVASCRIPT_MARKER = "border:2px solid red;";
	public final static String DYNAMIC_ELEMENT = "dynamic element";
	public static final String CUMULATIVE_RESULT_DETAILED_FILENAME = "cumulative_result_detailed.txt";
	public static final String CUMULATIVE_RESULT_SUMMARY_FILENAME = "cumulative_result_summary.txt";
	public static final int ERRORS_PER_FILE_COUNT = 1;
	public static final int ATTRIBUTES_PER_ELEMENT = 1;
	public static final String BASE_PATH = "evaluation";
	public static final String[] NON_TEXT_TAGS = new String[] {"img", "area", "audio", "video", "iframe", "meter", "progress", "canvas"};
	public static final String[] NON_VISUAL_TAGS = new String[] {"head", "script", "link", "meta", "style", "title"};
	public static final String SEED_ERRORS_XML_FILENAME = "seed_errors.xml";
	public static final String SEED_ERRORS_DTD_FILENAME = "seed_errors.dtd";
	public static final String SEED_ERRORS_DTD_FILENAME_WITH_PATH = "evaluationframework" + File.separatorChar + "seed_errors.dtd";
	public static final String TEST_RUN_DIRECTORY_NAME = "test_run_";
	public static final boolean HEURISTIC_1_ON = true;
	public static final boolean HEURISTIC_2_ON = true;
	public static final boolean HEURISTIC_3_ON = true;
	public static final boolean HEURISTIC_4_ON = true;
	public static final boolean HEURISTIC_5_ON = true;
	public static final String FILTERED_COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME = "filtered_";
	public static final boolean DO_NOT_OVERWRITE = true;
	public static final int RESULTS_TABLE_SIZE = 130;
	public static final String RESULT_FILE_TIME_REQUIRED_LINE = "Time required to run this test case = ";
	public static final String TIME_REQUIRED_UNIT = "sec";
	public static final String RESULT_FILE_PRIORITY_VALUE_SEPARATOR = ": ";
	public static final String CROPPED_IMAGE_NAME = "crop." + IMAGE_EXTENSION;
	public static final String DIRECTORY_TO_STORE_CROP_IMAGES = "crops";
	public static final String WINDOWS_FILE_ENCLOSING_CHARACTER_IMAGE_PROCESSING = "\"";
	public static final double WEIGHT_FOR_RATIO_CALCULATION = 1;
	public static final double WEIGHT_FOR_CASCADING_CALCULATION = 0.5;
	public static final double WEIGHT_FOR_HEURISTIC1 = 0.5;
	public static final double WEIGHT_FOR_HEURISTIC2 = 1;
	public static final double WEIGHT_FOR_NORMALIZED_DOM_HEIGHT = 0.5;
	public static final String IMAGEMAGICK_COMPARE_MINUS_SRC = "Minus_Src";
	public static final String IMAGEMAGICK_COMPARE_MINUS_DST = "Minus_Dst";
	public static final String IMAGEMAGICK_COMPARE_DIFFERENCE = "Difference";
	public static final String SPECIAL_REGIONS_XML_FILENAME = "special_regions.xml";
	public static final String TEXT_REGION_COPY_FILENAME = "text_region.html";
	public static final String PREDEFINED_CSS_PROPERTIES_FILE_PATH = "src/main/resources/predefined_css_properties.properties";
	public static final String RCA_COLOR_CATEGORY = "color";
	public static final String RCA_PREDEFINED_CATEGORY = "predefined";
	public static final String RCA_NUMERIC_POSITIVE_ONLY_CATEGORY = "numericPositive";
	public static final String RCA_NUMERIC_POSITIVE_NEGATIVE_CATEGORY = "numericPositiveNegative";
	public static final String RCA_PREDEFINED_NUMERIC_POSITIVE_ONLY_CATEGORY = "predefinedNumericPositive";
	public static final String RCA_PREDEFINED_NUMERIC_POSITIVE_NEGATIVE_CATEGORY = "predefinedNumericPositiveNegative";
	public static final String RCA_PREDEFINED_STRING_CATEGORY = "predefinedString";
	public static final String RCA_BOOLEAN_CATEGORY = "boolean";
	public static final String RCA_STRING_CATEGORY = "string";
	public static final String RCA_PROPERTIES_CATEGORIZATION_FILE_PATH = "src/main/resources/properties_categorization.properties";
	public static String SHORTHAND_CSS_PROPERTIES_FILE_PATH = "src/main/resources/shorthand_css_properties.properties";
	public static final int RCA_COLOR_ANALYSIS_PIXEL_SUB_SAMPLING_RATE = 1;
	public static final int GENETIC_ALGORITHM_INITIAL_POPULATION_SIZE_DIVIDING_FACTOR = 2;
	public static final String RCA_FIX_FOUND = "FIX_FOUND";
	public static final String RCA_FIX_NOT_FOUND = "FIX_NOT_FOUND";
	public static final String RCA_PROPERTY_TIMED_OUT = "TIMEDOUT";
	public static final int GENETIC_ALGORITHM_MAX_EVOLUTIONS = 200;
	public static final int GENETIC_ALGORITHM_MAX_POPLUATION_SIZE = 20;
	public static final int GENETIC_ALGORITHM_TIMEOUT_IN_MINS = 2;
	public static final int RCA_TIMEOUT_IN_MINS = 30;
	public static final int RCA_PER_ELEMENT_TIMEOUT_IN_MINS = 30;
	public static final int RCA_PER_PROPERTY_TIMEOUT_IN_MINS = 30;
	public static final int SIMULATED_ANNEALING_STARTING_TEMPERATURE = 50;
	public static final String WEBDRIVER_BROWSER = "FIREFOX";
	public static final int IMAGE_COMPARISON_PIXEL_SUB_SAMPLING_RATE = 1;
	public static final int NUMERIC_ANALYSIS_TRANSLATION_SUB_SAMPLING_VALUE = 1;
	public static final int RCA_WEBSEE_RANKING_BASED_ELEMENTS_CUTOFF = 10;
	public static final int RCA_NUMERIC_ANALYSIS_RATE_OF_CHANGE_WINDOW_SIZE = 50;
	public static final boolean HEADLESS_FIREFOX = false;
	
	// this value should specify at a minimum how much reduction in the difference pixels do you expect
	// reduction = 100% => exact match, no tolerance
	// reduction = 0% => any match, full tolerance
	public static final int RCA_NUMERIC_ANALYSIS_REDUCTION_IN_DIFFERENCE_PIXELS_THRESHOLD_PERCENTAGE = 100;
	
	public static final int RCA_SBST_SEARCH_SPACE_SIZE = 200;
	
	// PHANTOM JS
	public static final String PHANTOM_JS_EXECUTABLE_PATH = "/Users/sonal/USC/visual_checking/phantomjs-2.0.0-macosx/bin/phantomjs";
	
	// APACHE
	public static final String APACHE_DEPLOYMENT_PATH = "/Users/sonal/USC/visual_checking/apache/apache-tomcat-8.0.21/webapps";
	public static final String APACHE_DEPLOY_FILE_PATH = APACHE_DEPLOYMENT_PATH + File.separatorChar + "RCA" + File.separatorChar + "test.html";
	public static final String APACHE_DEPLOYED_FILE_HOST = "http://localhost:8080/RCA/test.html";
	
	public static ArrayList<String> getHtmlRelativeUrlsTagsAttributes()
	{
		ArrayList<String> htmlRelativeUrlsTagsAttributes = new ArrayList<String>();
		
		//html
		htmlRelativeUrlsTagsAttributes.add("src");
		htmlRelativeUrlsTagsAttributes.add("href");
		
		//css
		
		return htmlRelativeUrlsTagsAttributes;
	}
}
