/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import csiro.pidsvc.helper.URI;

/**
 * Implementation class of a formal grammar used in URI rewrite actions.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
@RunWith(JMock.class)
public class TestFormalGrammar
{
	private Mockery _mockContext = new Mockery();

//	@Test
//	public void test() throws Throwable
//	{
//		final HttpServletRequest mkRequest = _mockContext.mock(HttpServletRequest.class);
//		final HttpServletResponse mkResponse = _mockContext.mock(HttpServletResponse.class);
//		final ServletOutputStream sos = new ServletOutputStream() {
//			@Override
//			public void write(int c) throws IOException
//			{
//				System.out.print((char)c);
//			}
//		};
//	
//		_mockContext.checking(new Expectations() {
//			{
//				allowing(mkRequest).getInputStream(); will(returnValue(new ByteArrayInputStream("Test Mock".getBytes())));                
//				allowing(mkResponse).getOutputStream(); will(returnValue(sos));
//			}
//		});
//	}

	@Test
	public void testParseBucks0NonUrlSafe() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals("/testuri", grammar.parse("$0", false));
	}

	@Test
	public void testParseBucks0UrlSafe() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals("%2Ftesturi", grammar.parse("$0", true));
	}

	@Test
	public void testParseBucks1UrlSafe() throws Throwable
	{
		Pattern matchAuxiliaryData = Pattern.compile("^/(\\d+)$");

		FormalGrammar grammar = new FormalGrammar(URI.create("/100"), null, matchAuxiliaryData, null);
		Assert.assertEquals("100", grammar.parse("$1", true));
	}

	@Test
	public void testParseRaw() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/url?param1=1&param2=2&url=/testuri",
			grammar.parse("http://example.org/url?param1=1&param2=2&url=${RAW:$0}", true)
		);
	}

	@Test
	public void testParseRawNested() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/url?param1=1&param2=2&url=/testuri",
			grammar.parse("http://example.org/url?param1=1&param2=2&url=${RAW:${RAW:$0}}", true)
		);
	}

	@Test
	public void testParseEvnFn_REQUEST_URI() throws Throwable
	{
		Pattern matchAuxiliaryData = Pattern.compile("^/(.+)$");

		FormalGrammar grammar = new FormalGrammar(URI.create("/REQUEST_URI?querystring=1"), null, matchAuxiliaryData, null);
		Assert.assertEquals(
			"/REQUEST_URI",
			grammar.parse("${ENV:$1}", false)
		);
	}

	@Test
	public void testParseEvnFn_ORIGINAL_URI() throws Throwable
	{
		Pattern matchAuxiliaryData = Pattern.compile("^/(.+)$");

		FormalGrammar grammar = new FormalGrammar(URI.create("/ORIGINAL_URI?querystring=1"), null, matchAuxiliaryData, null);
		Assert.assertEquals(
			"%2FORIGINAL_URI%3Fquerystring%3D1",
			grammar.parse("${ENV:$1}", true)
		);
	}

	@Test
	public void testParseRawInEvnFn_ORIGINAL_URI() throws Throwable
	{
		Pattern matchAuxiliaryData = Pattern.compile("^/(.+)$");

		FormalGrammar grammar = new FormalGrammar(URI.create("/ORIGINAL_URI?querystring=1"), null, matchAuxiliaryData, null);
		Assert.assertEquals(
			"%2FORIGINAL_URI%3Fquerystring%3D1",
			grammar.parse("${ENV:${RAW:$1}}", true)
		);
	}

	@Test
	public void testParseEvnFn_FULL_REQUEST_URI_BASE() throws Throwable
	{
		final HttpServletRequest mkRequest = _mockContext.mock(HttpServletRequest.class);
	
		_mockContext.checking(new Expectations() {
			{
				allowing(mkRequest).getScheme(); will(returnValue("http"));
				allowing(mkRequest).getServerName(); will(returnValue("example.org"));
				allowing(mkRequest).getServerPort(); will(returnValue(8080));
			}
		});

		FormalGrammar grammar = new FormalGrammar(URI.create("/id/test"), mkRequest, null, null);
		Assert.assertEquals(
			"http://example.org:8080/id/",
			grammar.parse("${RAW:${ENV:FULL_REQUEST_URI_BASE}}", true)
		);

		grammar = new FormalGrammar(URI.create("/id/"), mkRequest, null, null);
		Assert.assertEquals(
			"http://example.org:8080/id/",
			grammar.parse("${RAW:${ENV:FULL_REQUEST_URI_BASE}}", true)
		);

		grammar = new FormalGrammar(URI.create("/"), mkRequest, null, null);
		Assert.assertEquals(
			"http://example.org:8080/",
			grammar.parse("${RAW:${ENV:FULL_REQUEST_URI_BASE}}", true)
		);
	}

	@Test
	public void testParseEvnFn_FILENAME() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/id/test.ext"), null, null, null);
		Assert.assertEquals(
			"test",
			grammar.parse("${ENV:FILENAME}", true)
		);

		grammar = new FormalGrammar(URI.create("/id/test"), null, null, null);
		Assert.assertEquals(
			"test",
			grammar.parse("${ENV:FILENAME}", true)
		);

		grammar = new FormalGrammar(URI.create("/id/"), null, null, null);
		Assert.assertEquals(
			"",
			grammar.parse("${ENV:FILENAME}", true)
		);
	}

	@Test
	public void testParseEvnFn_FILENAME_EXT() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/id/test.ext"), null, null, null);
		Assert.assertEquals(
			"test.ext",
			grammar.parse("${ENV:FILENAME_EXT}", true)
		);

		grammar = new FormalGrammar(URI.create("/id/test"), null, null, null);
		Assert.assertEquals(
			"test",
			grammar.parse("${ENV:FILENAME_EXT}", true)
		);

		grammar = new FormalGrammar(URI.create("/id/"), null, null, null);
		Assert.assertEquals(
			"",
			grammar.parse("${ENV:FILENAME_EXT}", true)
		);
	}

	@Test
	public void testParseSpacesInQuerystring() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/ORIGINAL_URI?querystring=test string"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=test+string",
			grammar.parse("http://example.org/?q=${QS:querystring}", true)
		);
	}

	@Test
	public void testParseSpacesInQuerystringRaw() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/ORIGINAL_URI?querystring=test%20string"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=test string",
			grammar.parse("http://example.org/?q=${RAW:${QS:querystring}}", true)
		);
	}

	@Test
	public void testParseFnCGroupZero() throws Throwable
	{
		Pattern re = Pattern.compile(".*(xml|html).*", Pattern.CASE_INSENSITIVE);
		Matcher m = re.matcher("test xml string");

		Assert.assertEquals(true, m.matches());

		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, m);
		Assert.assertEquals(
			"http://example.org/?q=test+xml+string",
			grammar.parse("http://example.org/?q=${C:0}", true)
		);
	}

	@Test
	public void testParseFnCGroupOmitted() throws Throwable
	{
		Pattern re = Pattern.compile(".*(xml|html).*", Pattern.CASE_INSENSITIVE);
		Matcher m = re.matcher("test xml string");

		Assert.assertEquals(true, m.matches());

		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, m);
		Assert.assertEquals(
			"http://example.org/?q=test+xml+string",
			grammar.parse("http://example.org/?q=${C}", true)
		);
	}

	@Test
	public void testParseFnCGroupOne() throws Throwable
	{
		Pattern re = Pattern.compile(".*(xml|html).*", Pattern.CASE_INSENSITIVE);
		Matcher m = re.matcher("test xml string");

		Assert.assertEquals(true, m.matches());

		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, m);
		Assert.assertEquals(
			"http://example.org/?q=xml",
			grammar.parse("http://example.org/?q=${C:1}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositive() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=YES",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=test:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElseNegative() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?qs=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=NO",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=test:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveEscapeCharactersColon() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test%3Atest"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=YES",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=test\\:test:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveEscapeCharactersEqSign() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test%3Dtest"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=YES",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=test\\=test:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveEscapeCharactersAmpSign() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test%26test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=YES",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=test\\&test:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveEscapeCharactersAmpSignAndOp() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test%26test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=YES",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=test\\&test&1\\{=1\\{:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveNoComparisonOperand() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=YES",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveNoComparisonOperandNull() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=NO",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElsePositiveEqualsToEmpty() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=NO",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:${QS:querystring}=:YES:NO}", true)
		);
	}

	@Test
	public void testParseFnIfThenElseNegativeNoElse() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=",
			grammar.parse("http://example.org/?q=${IF_THEN_ELSE:1=2:YES}", true)
		);
	}

	@Test
	public void testParseFnIsNullPositive() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=SUBSTITUTION",
			grammar.parse("http://example.org/?q=${ISNULL:${QS:querystring}:SUBSTITUTION}", true)
		);
	}

	@Test
	public void testParseFnIsNullNegative() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=test",
			grammar.parse("http://example.org/?q=${ISNULL:${QS:querystring}:SUBSTITUTION}", true)
		);
	}

	@Test
	public void testParseFnNullIfPositive() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=",
			grammar.parse("http://example.org/?q=${NULLIF:${QS:querystring}:test}", true)
		);
	}

	@Test
	public void testParseFnNullIfNegative() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=NEGATIVE"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=NEGATIVE",
			grammar.parse("http://example.org/?q=${NULLIF:${QS:querystring}:test}", true)
		);
	}

	@Test
	public void testParseFnCoalesce0() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=TWO",
			grammar.parse("http://example.org/?q=${COALESCE::TWO:THREE}", true)
		);
	}

	@Test
	public void testParseFnCoalesce1() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=THREE",
			grammar.parse("http://example.org/?q=${COALESCE:::THREE}", true)
		);
	}

	@Test
	public void testParseFnCoalesce2() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=",
			grammar.parse("http://example.org/?q=${COALESCE:::}", true)
		);
	}

	@Test
	public void testParseFnCoalesce3() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=",
			grammar.parse("http://example.org/?q=${COALESCE}", true)
		);
	}

	@Test
	public void testParseFnLowerCase() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=TEST"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=test",
			grammar.parse("http://example.org/?q=${LOWERCASE:${QS:querystring}}", true)
		);
	}

	@Test
	public void testParseFnUpperCase() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=TEST",
			grammar.parse("http://example.org/?q=${UPPERCASE:${QS:querystring}}", true)
		);
	}

	@Test
	public void testParseSuppressingMissingQsParametersPositive() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri?querystring=test"), null, null, null);
		Assert.assertEquals(
			"http://example.org/?q=test",
			grammar.parse("http://example.org/${RAW:${IF_THEN_ELSE:${QS:querystring}:?q\\=${QS:querystring}}}", true)
		);
	}

	@Test
	public void testParseSuppressingMissingQsParametersNegative() throws Throwable
	{
		FormalGrammar grammar = new FormalGrammar(URI.create("/testuri"), null, null, null);
		Assert.assertEquals(
			"http://example.org/",
			grammar.parse("http://example.org/${RAW:${IF_THEN_ELSE:${QS:querystring}:?q\\=${QS:querystring}}}", true)
		);
	}
}
