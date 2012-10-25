<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xpath-default-namespace="urn:csiro:xmlns:pidsvc:mapping:1.0">
	<xsl:output method="text" version="1.0" encoding="UTF-8"/>

	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="mapping">
				<!-- Single mapping import -->
				BEGIN;
				<xsl:apply-templates select="mapping"/>
				COMMIT;
			</xsl:when>
			<xsl:when test="backup[@type = 'partial' and @scope = 'record']">
				<!-- Partial mapping import -->
				<xsl:value-of select='concat("--OK: Successfully imported [[[", replace(replace(backup/mapping[1]/path, "&#39;", "&#39;&#39;"), "\\", "\\\\"), "]]].")'/>
				BEGIN;
				<xsl:apply-templates select="backup/mapping[1]"/>
				COMMIT;
			</xsl:when>
			<xsl:when test="backup[@type = 'full' and @scope = 'record']">
				<!-- Full mapping import -->
				<xsl:variable name="path" select="backup/mapping[1]/path/text()"/>
				<xsl:value-of select='concat("--OK: Successfully imported [[[", replace(replace($path, "&#39;", "&#39;&#39;"), "\\", "\\\\"), "]]].")'/>
				BEGIN;
				DELETE FROM "mapping" WHERE mapping_path = E'<xsl:value-of select='replace(replace($path, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>';
				<xsl:apply-templates select="backup/mapping[path/text() = $path]"/>
				COMMIT;
			</xsl:when>
			<xsl:when test="backup[@scope = 'db']">
				<!-- Full/Partial db restore -->
				<xsl:value-of select="concat('--OK: Successfully imported ', count(distinct-values(/backup/mapping/path/text())), ' record(s).')"/>
				BEGIN;
				<xsl:call-template name="cleanup"/>
				<xsl:apply-templates select="backup/mapping"/>
				COMMIT;
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="cleanup">
		<xsl:for-each select="distinct-values(/backup/mapping/path/text())">
			DELETE FROM "mapping" WHERE mapping_path = E'<xsl:value-of select='replace(replace(., "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>';
		</xsl:for-each>
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

		UPDATE "mapping" SET date_end = now() WHERE mapping_path = E'<xsl:value-of select="$path"/>' AND date_end IS NULL;

		<xsl:apply-templates select="action" mode="default_action"/>

		<xsl:choose>
			<xsl:when test="/backup">
				<!-- Allow date_start/date_end restore for backups only. -->
				INSERT INTO "mapping" (mapping_path, description, creator, "type", default_action_id<xsl:if test="@date_start">, date_start</xsl:if><xsl:if test="@date_end">, date_end</xsl:if>)
				VALUES (E'<xsl:value-of select="$path"/>', <xsl:value-of select="$description"/>, <xsl:value-of select="$creator"/>, '<xsl:value-of select="type"/>', <xsl:value-of select="$default_action_id"/>
					<xsl:if test="@date_start">, '<xsl:value-of select="@date_start"/>'</xsl:if>
					<xsl:if test="@date_end">, '<xsl:value-of select="@date_end"/>'</xsl:if>);
			</xsl:when>
			<xsl:otherwise>
				INSERT INTO "mapping" (mapping_path, description, creator, "type", default_action_id)
				VALUES (E'<xsl:value-of select="$path"/>', <xsl:value-of select="$description"/>, <xsl:value-of select="$creator"/>, '<xsl:value-of select="type"/>', <xsl:value-of select="$default_action_id"/>);
			</xsl:otherwise>
		</xsl:choose>

		<xsl:apply-templates select="conditions"/>
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
