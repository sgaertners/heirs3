<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" version="1.0" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/brief">
        <fo:root font-family="Helvetica, Arial, sans-serif" font-size="10pt" line-height="1.4">

            <fo:layout-master-set>
                <fo:simple-page-master master-name="din-a4"
                                       page-width="210mm" page-height="297mm"
                                       margin-top="0mm" margin-bottom="0mm"
                                       margin-left="0mm" margin-right="0mm">

                    <!-- Textbereich: Start bei 103.5 mm für den Betreff -->
                    <fo:region-body margin-top="103.5mm" margin-bottom="25mm"
                                    margin-left="25mm" margin-right="20mm"/>

                    <!-- Feste Fußzeile für Firmen-/Bankdaten -->
                    <fo:region-after extent="22mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="din-a4">

                <!-- 1. FUSSZEILE (Bankverbindungen etc.) -->
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block margin-left="25mm" margin-right="20mm" font-size="7.5pt" color="#555555"   border-top="0.5pt solid #cccccc" padding-top="2mm">
<!--                        <fo:table table-layout="fixed" width="100%">-->
<!--                            <fo:table-column column-width="33%"/>-->
<!--                            <fo:table-column column-width="33%"/>-->
<!--                            <fo:table-column column-width="34%"/>-->
<!--                            <fo:table-body>-->
<!--                                <fo:table-row>-->
                                    <xsl:for-each select="fusszeile/spalte">
<!--                                        <fo:table-cell>-->
                                            <fo:block line-height="1.3" text-align="start">
                                                <xsl:apply-templates select="node()"/>
                                            </fo:block>
<!--                                        </fo:table-cell>-->
                                    </xsl:for-each>
<!--                                </fo:table-row>-->
<!--                            </fo:table-body>-->
<!--                        </fo:table>-->
                    </fo:block>
                </fo:static-content>

                <!-- 2. HAUPTINHALT & ABSOLUT PLATZIERTE DIN-ELEMENTE -->
                <fo:flow flow-name="xsl-region-body">

                    <!-- ========================================== -->
                    <!-- DIN 5008: FALZ- UND LOCHMARKEN             -->
                    <!-- ========================================== -->
                    <!-- Falzmarke 1 (105mm) -->
                    <fo:block-container absolute-position="fixed" top="105mm" left="0mm" width="5mm" height="0.2mm">
                        <fo:block border-bottom="0.5pt solid #888888"/>
                    </fo:block-container>

                    <!-- Lochmarke Mitte (148.5mm) -->
                    <fo:block-container absolute-position="fixed" top="148.5mm" left="0mm" width="7mm" height="0.2mm">
                        <fo:block border-bottom="0.5pt solid #888888"/>
                    </fo:block-container>

                    <!-- Falzmarke 2 (210mm) -->
                    <fo:block-container absolute-position="fixed" top="210mm" left="0mm" width="5mm" height="0.2mm">
                        <fo:block border-bottom="0.5pt solid #888888"/>
                    </fo:block-container>


                    <!-- ========================================== -->
                    <!-- SENDER LOGO / KOPFZEILE (bis 45mm Höhe)    -->
                    <!-- ========================================== -->
                    <fo:block-container absolute-position="fixed" top="20mm" left="25mm" width="165mm" height="20mm">
                        <fo:block font-size="16pt" font-weight="bold" text-align="right" color="#1a3b5d">
                            <xsl:value-of select="absender/firma"/>
                        </fo:block>
                    </fo:block-container>


                    <!-- ========================================== -->
                    <!-- ANSCHRIFTFELD FORM B (Start: 45mm, B: 85mm)-->
                    <!-- ========================================== -->
                    <!-- Rücksendeangabe (Zusatz- u. Vermerkzone: 5mm Höhe) -->
                    <fo:block-container absolute-position="fixed" top="45mm" left="20mm" width="85mm" height="5mm">
                        <fo:block font-size="7pt" text-decoration="underline" color="#444444">
                            <xsl:value-of select="absender/ruecksendezeile"/>
                        </fo:block>
                    </fo:block-container>

                    <!-- Empfänger-Adresse (ab 50mm, max. 40mm Höhe) -->
                    <fo:block-container absolute-position="fixed" top="50mm" left="20mm" width="85mm" height="40mm">
                        <xsl:for-each select="empfaenger/zeile">
                            <fo:block font-size="10pt"><xsl:value-of select="."/></fo:block>
                        </xsl:for-each>
                    </fo:block-container>


                    <!-- ========================================== -->
                    <!-- INFORMATIONSBLOCK RECHTS (ab 50mm, X:125mm)-->
                    <!-- ========================================== -->
                    <fo:block-container absolute-position="fixed" top="50mm" left="125mm" width="65mm">
                        <fo:table table-layout="fixed" width="100%" font-size="8.5pt">
                            <fo:table-column column-width="30mm"/>
                            <fo:table-column column-width="35mm"/>
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">Ihr Zeichen:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="metadaten/ihr-zeichen"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">Ihre Nachricht:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="metadaten/ihre-nachricht-vom"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">Unser Zeichen:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="metadaten/unser-zeichen"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">Kunden-Nr.:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="metadaten/kunden-nr"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">Name:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="absender/ansprechpartner"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">Telefon:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="absender/telefon"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell><fo:block color="#666666">E-Mail:</fo:block></fo:table-cell>
                                    <fo:table-cell><fo:block><xsl:value-of select="absender/email"/></fo:block></fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell padding-top="2mm"><fo:block color="#666666">Datum:</fo:block></fo:table-cell>
                                    <fo:table-cell padding-top="2mm"><fo:block font-weight="bold"><xsl:value-of select="metadaten/datum"/></fo:block></fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block-container>


                    <!-- ========================================== -->
                    <!-- REGULÄRER BRIEFTEXT (fließt natürlich ab)  -->
                    <!-- ========================================== -->

                    <!-- Betreffzeile -->
                    <fo:block font-weight="bold" font-size="11pt" space-after="6mm">
                        <xsl:value-of select="inhalt/betreff"/>
                    </fo:block>

                    <!-- Anrede -->
                    <fo:block space-after="4mm">
                        <xsl:value-of select="inhalt/anrede"/>
                    </fo:block>

                    <!-- Absätze -->
                    <xsl:for-each select="inhalt/absatz">
                        <fo:block space-after="3.5mm" text-align="justify">
                            <xsl:value-of select="."/>
                        </fo:block>
                    </xsl:for-each>

                    <!-- Grußformel (Zusammenhalten mit Unterschrift) -->
                    <fo:block keep-with-next.within-page="always" space-before="5mm" space-after="15mm">
                        <xsl:value-of select="inhalt/grussformel"/>
                    </fo:block>

                    <!-- Unterschrift / Name -->
                    <fo:block keep-with-next.within-page="always">
                        <fo:block font-weight="bold"><xsl:value-of select="inhalt/unterschrift/name"/></fo:block>
                        <fo:block font-size="9pt" color="#555555"><xsl:value-of select="inhalt/unterschrift/position"/></fo:block>
                    </fo:block>

                    <!-- Anlagenvermerk (optional) -->
                    <xsl:if test="inhalt/anlagen/anlage">
                        <fo:block space-before="10mm" font-size="9pt">
                            <fo:block font-weight="bold" space-after="1mm">Anlagen</fo:block>
                            <xsl:for-each select="inhalt/anlagen/anlage">
                                <fo:block><xsl:value-of select="."/></fo:block>
                            </xsl:for-each>
                        </fo:block>
                    </xsl:if>

                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <!-- Template zur Verarbeitung von HTML-ähnlichen <br/> in XML-Tags -->
    <xsl:template match="br">
        <fo:block/>
    </xsl:template>

</xsl:stylesheet>