<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:pidsvc="urn:csiro:xmlns:pidsvc:backup:1.0">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:param name="replace" select="0"/>

	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="not(count(merge/source//pidsvc:mapping) = 1 or (count(merge/source//pidsvc:mapping) = 0 and count(merge/source//pidsvc:conditions) = 1))">
				<error>ERROR: The source configuration must contain one mapping only.</error>
			</xsl:when>
			<xsl:otherwise>
				<response>
					<log>
						<xsl:apply-templates select="/merge/target/pidsvc:backup/pidsvc:mapping/pidsvc:conditions/pidsvc:condition" mode="Log"/>
						<xsl:apply-templates select="/merge/source//pidsvc:conditions/pidsvc:condition" mode="LogSourceCopy"/>
					</log>
					<merged>
						<xsl:apply-templates select="merge/target/*"/>
					</merged>
				</response>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="pidsvc:backup" priority="1">
		<!-- Drop pidsvc:backup element. -->
		<xsl:apply-templates select="pidsvc:mapping[1]"/>
	</xsl:template>
	<xsl:template match="pidsvc:mapping" priority="1">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
			<xsl:if test="not(pidsvc:conditions) and /merge/source//pidsvc:conditions/pidsvc:condition">
				<conditions xmlns="urn:csiro:xmlns:pidsvc:backup:1.0">
					<xsl:apply-templates select="/merge/source//pidsvc:conditions/pidsvc:condition" mode="SourceCopy"/>
				</conditions>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="pidsvc:conditions" priority="1">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
			<xsl:apply-templates select="/merge/source//pidsvc:conditions/pidsvc:condition" mode="SourceCopy"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="pidsvc:condition" priority="1">
		<xsl:choose>
			<xsl:when test="$replace = 1 and preceding-sibling::pidsvc:condition[pidsvc:type/text() = current()/pidsvc:type/text() and pidsvc:match/text() = current()/pidsvc:match/text()]"/>
			<xsl:when test="$replace = 1">
				<xsl:variable name="source" select="/merge/source//pidsvc:conditions/pidsvc:condition[pidsvc:type/text() = current()/pidsvc:type/text() and pidsvc:match/text() = current()/pidsvc:match/text()]"/>
				<xsl:choose>
					<xsl:when test="count($source) > 0">
						<xsl:copy-of select="$source[1]"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy>
							<xsl:apply-templates select="@* | node()"/>
						</xsl:copy>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="@* | node()"/>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="pidsvc:condition" priority="1" mode="SourceCopy">
		<xsl:if test="$replace = 0 or not(/merge/target/pidsvc:backup/pidsvc:mapping/pidsvc:conditions/pidsvc:condition[pidsvc:type/text() = current()/pidsvc:type/text() and pidsvc:match/text() = current()/pidsvc:match/text()])">
			<xsl:copy>
				<xsl:apply-templates select="@* | node()"/>
			</xsl:copy>
		</xsl:if>
	</xsl:template>
	<xsl:template match="attribute() | element() | text() | comment() | processing-instruction()" priority="0">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>

	<!-- Log -->
	<xsl:template match="pidsvc:condition" priority="1" mode="Log">
		<xsl:choose>
			<xsl:when test="$replace = 1 and preceding-sibling::pidsvc:condition[pidsvc:type/text() = current()/pidsvc:type/text() and pidsvc:match/text() = current()/pidsvc:match/text()]"/>
			<xsl:when test="$replace = 1">
				<xsl:variable name="source" select="/merge/source//pidsvc:conditions/pidsvc:condition[pidsvc:type/text() = current()/pidsvc:type/text() and pidsvc:match/text() = current()/pidsvc:match/text()]"/>
				<xsl:choose>
					<xsl:when test="count($source) > 0">
						<condition>1</condition>
					</xsl:when>
					<xsl:otherwise>
						<condition>0</condition>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<condition>0</condition>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="pidsvc:condition" priority="1" mode="LogSourceCopy">
		<xsl:if test="$replace = 0 or not(/merge/target/pidsvc:backup/pidsvc:mapping/pidsvc:conditions/pidsvc:condition[pidsvc:type/text() = current()/pidsvc:type/text() and pidsvc:match/text() = current()/pidsvc:match/text()])">
			<condition>1</condition>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
