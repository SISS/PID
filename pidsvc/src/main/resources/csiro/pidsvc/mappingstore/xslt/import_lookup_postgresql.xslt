<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xpath-default-namespace="urn:csiro:xmlns:pidsvc:lookup:1.0">
	<xsl:output method="text" version="1.0" encoding="UTF-8"/>

	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="backup">
				<!-- Lookup maps backup -->
				<xsl:choose>
					<xsl:when test="backup/lookup">
						<xsl:value-of select='concat("--OK: Successfully imported ", count(backup/lookup), " lookup map(s).")'/>
					</xsl:when>
					<xsl:otherwise>--OK: Backup file is empty. No changes have been made.</xsl:otherwise>
				</xsl:choose>
				BEGIN;
				<xsl:apply-templates select="backup/lookup"/>
				COMMIT;
			</xsl:when>
			<xsl:when test="lookup/ns">
				<!-- Single lookup import -->
				<xsl:value-of select='concat("--OK: Successfully imported [[[", lookup/ns/text(), "]]].")'/>
				BEGIN;
				<xsl:apply-templates select="lookup"/>
				COMMIT;
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>--ERROR: Unrecognised namespace.</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="lookup">
		<xsl:variable name="ns">
			<xsl:value-of select='replace(replace(ns/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="type">
			<xsl:value-of select="local-name(*[last()])"/>
		</xsl:variable>
		<xsl:variable name="behaviour_value">
			<xsl:choose>
				<xsl:when test="not(default/text())">NULL</xsl:when>
				<xsl:otherwise>E'<xsl:value-of select='replace(replace(default/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>'</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:if test="ns/@rename">
			DELETE FROM lookup_ns WHERE ns = E'<xsl:value-of select='replace(replace(ns/@rename, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>';
		</xsl:if>
		DELETE FROM lookup_ns WHERE ns = E'<xsl:value-of select="$ns"/>';
		INSERT INTO lookup_ns (ns, type, behaviour_type, behaviour_value) VALUES (E'<xsl:value-of select="$ns"/>', E'<xsl:value-of select="$type"/>', E'<xsl:value-of select="default/@type"/>', <xsl:value-of select="$behaviour_value"/>);

		<xsl:apply-templates select="*[last()]"/>
	</xsl:template>

	<xsl:template match="Static">
		<xsl:apply-templates select="pair"/>
	</xsl:template>
	<xsl:template match="pair">
		<xsl:variable name="ns">
			<xsl:value-of select='replace(replace(../../ns/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="key">
			<xsl:value-of select='replace(replace(key/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="value">
			<xsl:value-of select='replace(replace(value/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		INSERT INTO lookup (ns, key, value) VALUES (E'<xsl:value-of select="$ns"/>', E'<xsl:value-of select="$key"/>', E'<xsl:value-of select="$value"/>');
	</xsl:template>

	<xsl:template match="HttpResolver">
		<xsl:variable name="ns">
			<xsl:value-of select='replace(replace(../ns/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="key">
			<xsl:value-of select='replace(replace(endpoint/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="type">
			<xsl:value-of select='replace(replace(type/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="extractor">
			<xsl:value-of select='replace(replace(extractor/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="namespaces">
			<xsl:apply-templates select="namespaces/ns"/>
		</xsl:variable>
		<xsl:variable name="value">
			<xsl:value-of select="concat('T:', $type, '&#10;E:', $extractor, $namespaces)"/>
		</xsl:variable>
		INSERT INTO lookup (ns, key, value) VALUES (E'<xsl:value-of select="$ns"/>', E'<xsl:value-of select="$key"/>', E'<xsl:value-of select="$value"/>');
	</xsl:template>
	<xsl:template match="ns">
		<xsl:value-of select="concat('&#10;NS:', @prefix, ':', text())"/>
	</xsl:template>
</xsl:stylesheet>
