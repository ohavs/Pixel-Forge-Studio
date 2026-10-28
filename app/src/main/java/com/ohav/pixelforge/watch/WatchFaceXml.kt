package com.ohav.pixelforge.watch

/** Minimal Watch Face Format (v1) face: ring, big digital time, label. Colors are ARGB hex ("#ffrrggbb"). */
object WatchFaceXml {
    fun simple(background: String, accent: String, label: String): String = """
<?xml version="1.0" encoding="utf-8"?>
<WatchFace width="450" height="450" clipShape="CIRCLE">
  <Metadata key="CLOCK_TYPE" value="DIGITAL"/>
  <Scene backgroundColor="$background">
    <Variant mode="AMBIENT" target="backgroundColor" value="#ff000000"/>
    <PartDraw x="0" y="0" width="450" height="450" name="ring" alpha="255">
      <Variant mode="AMBIENT" target="alpha" value="0"/>
      <Ellipse x="15" y="15" width="420" height="420">
        <Stroke color="$accent" thickness="6"/>
      </Ellipse>
    </PartDraw>
    <DigitalClock x="25" y="150" width="400" height="130">
      <TimeText x="0" y="0" width="400" height="130" format="hh:mm" hourFormat="SYNC_TO_DEVICE" align="CENTER">
        <Font family="SYNC_TO_DEVICE" size="110" weight="BOLD" slant="NORMAL" color="#ffffffff"/>
      </TimeText>
    </DigitalClock>
    <PartText x="25" y="290" width="400" height="50" name="label" alpha="255">
      <Text align="CENTER" ellipsis="TRUE">
        <Font family="SYNC_TO_DEVICE" size="26" weight="BOLD" slant="NORMAL" color="$accent">${escape(label)}</Font>
      </Text>
    </PartText>
  </Scene>
</WatchFace>
""".trimStart()

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
