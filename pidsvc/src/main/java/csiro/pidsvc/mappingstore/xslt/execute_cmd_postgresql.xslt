<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" version="1.0" encoding="UTF-8"/>
	
	<xsl:template match="/">
		<xsl:apply-templates select="mapping"/>
	</xsl:template>
	<xsl:template match="mapping">
		<xsl:variable name="path">
			<xsl:value-of select='replace(replace(path, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
		</xsl:variable>
		<xsl:variable name="description">
			<xsl:choose>
				<xsl:when test="description">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(description, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="creator">
			<xsl:choose>
				<xsl:when test="creator">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(creator, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="default_action_id">
			<xsl:choose>
				<xsl:when test="action">
					<xsl:text>(SELECT currval('"action_action_id_seq"'::regclass))</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		BEGIN;

		UPDATE "mapping" SET date_end = now() WHERE mapping_path = E'<xsl:value-of select="$path"/>' AND date_end IS NULL;

		<xsl:apply-templates select="action" mode="default_action"/>

		INSERT INTO "mapping" (mapping_path, description, creator, "type", default_action_id)
		VALUES (E'<xsl:value-of select="$path"/>', <xsl:value-of select="$description"/>, <xsl:value-of select="$creator"/>, '<xsl:value-of select="type"/>', <xsl:value-of select="$default_action_id"/>);
		
		<xsl:apply-templates select="conditions"/>

		COMMIT;
	</xsl:template>

	<xsl:template match="conditions">
		<xsl:apply-templates select="condition"/>
	</xsl:template>
	<xsl:template match="condition">
		INSERT INTO "condition" (mapping_id, "type", "match")
		VALUES ((SELECT currval('"mapping_mapping_id_seq"'::regclass)), '<xsl:value-of select="type"/>', E'<xsl:value-of select='replace(replace(match, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>');
		
		<xsl:apply-templates select="actions/action"/>
	</xsl:template>

	<xsl:template match="action" mode="default_action">
		<xsl:variable name="name">
			<xsl:choose>
				<xsl:when test="name">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(name, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="value">
			<xsl:choose>
				<xsl:when test="value">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(value, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		INSERT INTO "action" ("type", action_name, action_value)
		VALUES ('<xsl:value-of select="type"/>', <xsl:value-of select="$name"/>, <xsl:value-of select="$value"/>);
	</xsl:template>
	<xsl:template match="action">
		<xsl:variable name="name">
			<xsl:choose>
				<xsl:when test="name">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(name, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="value">
			<xsl:choose>
				<xsl:when test="value">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(value, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		INSERT INTO "action" ("type", condition_id, action_name, action_value)
		VALUES ('<xsl:value-of select="type"/>', (SELECT currval('"condition_condition_id_seq"'::regclass)), <xsl:value-of select="$name"/>, <xsl:value-of select="$value"/>);
	</xsl:template>

</xsl:stylesheet>
