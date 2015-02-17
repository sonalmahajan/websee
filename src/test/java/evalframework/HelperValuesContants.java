package evalframework;

import java.io.File;

public final class HelperValuesContants
{
	public static final String NON_ZERO_POSITIVE_INTEGER = "NON_ZERO_POSITIVE_INTEGER";
	public static final String INTEGER = "INTEGER";
	public static final String NON_ZERO_POSITIVE_DECIMAL = "NON_ZERO_POSITIVE_DECIMAL";
	public static final String DECIMAL = "DECIMAL";
	public static final String URL = "URL";
	public static final String TEXT = "TEXT";
	public static final int MIN_NON_ZERO_POSITIVE_INTEGER = 1;
	public static final int MAX_INTEGER = 10;
	public static final int MIN_INTEGER = -10;
	public static final float MIN_NON_ZERO_POSITIVE_DECIMAL = 1.0f;
	public static final float MAX_DECIMAL = 10.0f;
	public static final float MIN_DECIMAL = -10.0f;
	public static final int STRING_LENGTH = 10;
	public static final String DERIVED_VALUE = "\\<.*?\\>";
	public static final String DERIVED_VALUE_CHECK = ".*" + DERIVED_VALUE + "*.";
	public static final String STRING_RANDOM = "abcdefghijklmnopqrstuvewxyz";
	public static final String URL_IMAGE_PATH = "evaluationframework" + File.separatorChar + "test_image.gif";
	public static final String NORMALIZED_DECIMAL = "NORMALIZED_DECIMAL";
	public static final String URL_SMALL_IMAGE = "URL_SMALL_IMAGE";
	public static final String URL_SMALL_IMAGE_PATH = "evaluationframework" + File.separatorChar + "small_image.gif";
	public static final String URL_BORDER_IMAGE = "URL_BORDER_IMAGE";
	public static final String URL_BORDER_IMAGE_PATH = "evaluationframework" + File.separatorChar + "border_image.gif";
	public static final String BIG_INTEGER = "BIG_INTEGER";
	public static final int MIN_BIG_INTEGER = 80;
	public static final int MAX_BIG_INTEGER = 100;
}
