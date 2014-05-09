<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xpath-default-namespace="urn:csiro:xmlns:pidsvc:backup:1.0">
	<xsl:output method="text" version="1.0" encoding="UTF-8"/>

	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="backup">
				<!-- Condition set backup -->
				<xsl:choose>
					<xsl:when test="backup/conditionSet">
						<xsl:value-of select='concat("--OK: Successfully imported ", count(backup/conditionSet), " condition set(s).")'/>
						BEGIN;
						<xsl:apply-templates select="backup/conditionSet"/>
						COMMIT;
					</xsl:when>
					<xsl:otherwise>--OK: Backup file is empty. No condition sets have been restored.</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="conditionSet/name">
				<!-- Single set import -->
				<xsl:value-of select='concat("--OK: Successfully imported [set[[", conditionSet/name/text(), "]]].")'/>
				BEGIN;
				<xsl:apply-templates select="conditionSet"/>
				COMMIT;
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>--ERROR: Unrecognised format.</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="conditionSet">
		<xsl:variable name="name">
			<xsl:value-of select='replace(replace(name/text(), "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
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

		<xsl:if test="name/@rename">
			<xsl:variable name="rename_attr">
				<xsl:value-of select='replace(replace(name/@rename, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
			</xsl:variable>
			DELETE FROM condition_set WHERE name = E'<xsl:value-of select='$rename_attr'/>';

			<!-- Rename existing active references to this condition set -->
			UPDATE condition
			SET match = E'<xsl:value-of select="$name"/>'
			WHERE condition_id IN (
				SELECT c.condition_id
				FROM condition c
					INNER JOIN mapping m ON m.mapping_id = c.mapping_id
				WHERE c.type = 'ConditionSet' AND c.match = E'<xsl:value-of select='$rename_attr'/>' AND m.date_end IS NULL
			);
		</xsl:if>
		DELETE FROM condition_set WHERE name = E'<xsl:value-of select="$name"/>';
		INSERT INTO condition_set (name, description) VALUES (E'<xsl:value-of select="$name"/>', <xsl:value-of select="$description"/>);

		<xsl:apply-templates select="conditions"/>
	</xsl:template>

	<xsl:template match="conditions">
		<xsl:apply-templates select="condition"/>
	</xsl:template>
	<xsl:template match="condition">
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

		INSERT INTO condition (condition_set_id, type, match, description)
		VALUES ((SELECT currval('"condition_set_condition_set_id_seq"'::regclass)), '<xsl:value-of select="type"/>', E'<xsl:value-of select='replace(replace(match, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>', <xsl:value-of select="$description"/>);

		<xsl:apply-templates select="actions/action"/>
	</xsl:template>
	<xsl:template match="action">
		<xsl:variable name="name">
			<xsl:choose>
				<xsl:when test="name/text()">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(name, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="value">
			<xsl:choose>
				<xsl:when test="value/text()">
					<xsl:text>E'</xsl:text>
					<xsl:value-of select='replace(replace(value, "&#39;", "&#39;&#39;"), "\\", "\\\\")'/>
					<xsl:text>'</xsl:text>
				</xsl:when>
				<xsl:otherwise>NULL</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		INSERT INTO action (type, condition_id, action_name, action_value)
		VALUES ('<xsl:value-of select="type"/>', (SELECT currval('"condition_condition_id_seq"'::regclass)), <xsl:value-of select="$name"/>, <xsl:value-of select="$value"/>);
	</xsl:template>
</xsl:stylesheet>
