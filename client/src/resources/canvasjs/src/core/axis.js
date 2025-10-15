import CanvasJSObject from "./canvasjs";
import TextBlock from "./text_block";
import { extend, numberFormat, getLineDashArray } from "../helpers/utils";

function Axis(chart, options, type, position) {
  Axis.base.constructor.call(this, "Axis", options, chart.theme);

  this.chart = chart;
  this.canvas = chart.canvas;
  this.ctx = chart.ctx;
  this.maxWidth = 0;
  this.maxHeight = 0;
  this.intervalStartPosition = 0;
  this.labels = [];
  this._labels = null;

  this.dataInfo = {
    min: Infinity,
    max: -Infinity,
    viewPortMin: Infinity,
    viewPortMax: -Infinity,
    minDiff: Infinity,
  };

  if (type === "axisX") {
    this.sessionVariables = this.chart.sessionVariables[type];

    if (!this._options.interval) this.intervalType = null;
  } else {
    if (position === "left" || position === "top")
      this.sessionVariables = this.chart.sessionVariables["axisY"];
    else {
      this.sessionVariables = this.chart.sessionVariables["axisY2"];
    }
  }

  if (typeof this._options.titleFontSize === "undefined") {
    this.titleFontSize = this.chart.getAutoFontSize(this.titleFontSize);
  }

  if (typeof this._options.labelFontSize === "undefined") {
    this.labelFontSize = this.chart.getAutoFontSize(this.labelFontSize);
  }

  this.type = type;
  if (
    type === "axisX" &&
    (!options || typeof options.gridThickness === "undefined")
  )
    this.gridThickness = 0;

  this._position = position;

  this.lineCoordinates = {
    x1: null,
    y1: null,
    x2: null,
    y2: null,
    width: null,
  };
  {
    this.labelAngle = ((this.labelAngle % 360) + 360) % 360;

    if (this.labelAngle > 90 && this.labelAngle <= 270) this.labelAngle -= 180;
    else if (this.labelAngle > 180 && this.labelAngle <= 270)
      this.labelAngle -= 180;
    else if (this.labelAngle > 270 && this.labelAngle <= 360)
      this.labelAngle -= 360;
  }

  if (this._options.stripLines && this._options.stripLines.length > 0) {
    this.stripLines = [];

    for (var i = 0; i < this._options.stripLines.length; i++) {
      this.stripLines.push(
        new StripLine(
          this.chart,
          this._options.stripLines[i],
          chart.theme,
          ++this.chart._eventManager.lastObjectId,
          this,
        ),
      );
    }
  }

  this._titleTextBlock = null;

  if (
    !this.hasOptionChanged("viewportMinimum") &&
    !isNaN(this.sessionVariables.newViewportMinimum) &&
    this.sessionVariables.newViewportMinimum !== null
  )
    this.viewportMinimum = this.sessionVariables.newViewportMinimum;
  else this.sessionVariables.newViewportMinimum = null;

  if (
    !this.hasOptionChanged("viewportMaximum") &&
    !isNaN(this.sessionVariables.newViewportMaximum) &&
    this.sessionVariables.newViewportMaximum !== null
  )
    this.viewportMaximum = this.sessionVariables.newViewportMaximum;
  else this.sessionVariables.newViewportMaximum = null;

  if (this.minimum !== null && this.viewportMinimum !== null)
    this.viewportMinimum = Math.max(this.viewportMinimum, this.minimum);

  if (this.maximum !== null && this.viewportMaximum !== null)
    this.viewportMaximum = Math.min(this.viewportMaximum, this.maximum);

  this.trackChanges("viewportMinimum");
  this.trackChanges("viewportMaximum");
}

extend(Axis, CanvasJSObject);

Axis.prototype.createLabels = function () {
  var textBlock;
  var i = 0;
  var endPoint;

  var labelMaxWidth = 0;
  var labelMaxHeight = 0;
  var intervalInPixels = 0;

  if (this._position === "bottom" || this._position === "top") {
    intervalInPixels =
      (this.lineCoordinates.width /
        Math.abs(this.viewportMaximum - this.viewportMinimum)) *
      this.interval;

    if (this.labelAutoFit) {
      labelMaxWidth =
        typeof this._options.labelMaxWidth === "undefined"
          ? (intervalInPixels * 0.9) >> 0
          : this.labelMaxWidth;
    } else {
      labelMaxWidth =
        typeof this._options.labelMaxWidth === "undefined"
          ? (this.chart.width * 0.7) >> 0
          : this.labelMaxWidth;
    }

    labelMaxHeight =
      typeof this._options.labelWrap === "undefined" || this.labelWrap
        ? (this.chart.height * 0.5) >> 0
        : this.labelFontSize * 1.5;
  } else if (this._position === "left" || this._position === "right") {
    intervalInPixels =
      (this.lineCoordinates.height /
        Math.abs(this.viewportMaximum - this.viewportMinimum)) *
      this.interval;

    if (this.labelAutoFit) {
      labelMaxWidth =
        typeof this._options.labelMaxWidth === "undefined"
          ? (this.chart.width * 0.3) >> 0
          : this.labelMaxWidth;
    } else {
      labelMaxWidth =
        typeof this._options.labelMaxWidth === "undefined"
          ? (this.chart.width * 0.5) >> 0
          : this.labelMaxWidth;
    }

    labelMaxHeight =
      typeof this._options.labelWrap === "undefined" || this.labelWrap
        ? (intervalInPixels * 2) >> 0
        : this.labelFontSize * 1.5;
  }

  if (
    this.type === "axisX" &&
    this.chart.plotInfo.axisXValueType === "dateTime"
  ) {
    endPoint = addToDateTime(
      new Date(this.viewportMaximum),
      this.interval,
      this.intervalType,
    );

    for (
      i = this.intervalStartPosition;
      i < endPoint;
      addToDateTime(i, this.interval, this.intervalType)
    ) {
      var timeInMilliseconds = i.getTime();
      var text = this.labelFormatter
        ? this.labelFormatter({
            chart: this.chart,
            axis: this._options,
            value: i,
            label: this.labels[i] ? this.labels[i] : null,
          })
        : this.type === "axisX" && this.labels[timeInMilliseconds]
          ? this.labels[timeInMilliseconds]
          : dateFormat(i, this.valueFormatString, this.chart._cultureInfo);

      textBlock = new TextBlock(this.ctx, {
        x: 0,
        y: 0,
        maxWidth: labelMaxWidth,
        maxHeight: labelMaxHeight,
        angle: this.labelAngle,
        text: this.prefix + text + this.suffix,
        horizontalAlign: "left",
        fontSize: this.labelFontSize,
        fontFamily: this.labelFontFamily,
        fontWeight: this.labelFontWeight,
        fontColor: this.labelFontColor,
        fontStyle: this.labelFontStyle,
        textBaseline: "middle",
      });

      this._labels.push({
        position: i.getTime(),
        textBlock: textBlock,
        effectiveHeight: null,
      });
    }
  } else {
    endPoint = this.viewportMaximum;

    if (this.labels && this.labels.length) {
      var tempInterval = Math.ceil(this.interval);
      var tempStartPoint = Math.ceil(this.intervalStartPosition);
      var hasAllLabels = false;
      for (i = tempStartPoint; i < this.viewportMaximum; i += tempInterval) {
        if (this.labels[i]) {
          hasAllLabels = true;
        } else {
          hasAllLabels = false;
          break;
        }
      }

      if (hasAllLabels) {
        this.interval = tempInterval;
        this.intervalStartPosition = tempStartPoint;
      }
    }

    for (
      i = this.intervalStartPosition;
      i <= endPoint;
      i = parseFloat((i + this.interval).toFixed(14))
    ) {
      var text = this.labelFormatter
        ? this.labelFormatter({
            chart: this.chart,
            axis: this._options,
            value: i,
            label: this.labels[i] ? this.labels[i] : null,
          })
        : this.type === "axisX" && this.labels[i]
          ? this.labels[i]
          : numberFormat(i, this.valueFormatString, this.chart._cultureInfo);

      textBlock = new TextBlock(this.ctx, {
        x: 0,
        y: 0,
        maxWidth: labelMaxWidth,
        maxHeight: labelMaxHeight,
        angle: this.labelAngle,
        text: this.prefix + text + this.suffix,
        horizontalAlign: "left",
        fontSize: this.labelFontSize,
        fontFamily: this.labelFontFamily,
        fontWeight: this.labelFontWeight,
        fontColor: this.labelFontColor,
        fontStyle: this.labelFontStyle,
        textBaseline: "middle",
        borderThickness: 0,
      });

      this._labels.push({
        position: i,
        textBlock: textBlock,
        effectiveHeight: null,
      });
    }
  }

  for (var i = 0; i < this.stripLines.length; i++) {
    var stripLine = this.stripLines[i];

    textBlock = new TextBlock(this.ctx, {
      x: 0,
      y: 0,
      backgroundColor: stripLine.labelBackgroundColor,
      maxWidth: labelMaxWidth,
      maxHeight: labelMaxHeight,
      angle: this.labelAngle,
      text: stripLine.labelFormatter
        ? stripLine.labelFormatter({
            chart: this.chart,
            axis: this,
            stripLine: stripLine,
          })
        : stripLine.label,
      horizontalAlign: "left",
      fontSize: stripLine.labelFontSize,
      fontFamily: stripLine.labelFontFamily,
      fontWeight: stripLine.labelFontWeight,
      fontColor: stripLine._options.labelFontColor || stripLine.color,
      fontStyle: stripLine.labelFontStyle,
      textBaseline: "middle",
      borderThickness: 0,
    });

    this._labels.push({
      position: stripLine.value,
      textBlock: textBlock,
      effectiveHeight: null,
      stripLine: stripLine,
    });
  }
};

Axis.prototype.createLabelsAndCalculateWidth = function () {
  var maxLabelEffectiveWidth = 0;
  this._labels = [];

  if (this._position === "left" || this._position === "right") {
    this.createLabels();

    for (var i = 0; i < this._labels.length; i++) {
      var textBlock = this._labels[i].textBlock;

      var size = textBlock.measureText();

      var labelEffectiveWidth = 0;

      if (this.labelAngle === 0) labelEffectiveWidth = size.width;
      else
        labelEffectiveWidth =
          size.width * Math.cos((Math.PI / 180) * Math.abs(this.labelAngle)) +
          (size.height / 2) *
            Math.sin((Math.PI / 180) * Math.abs(this.labelAngle));

      if (maxLabelEffectiveWidth < labelEffectiveWidth)
        maxLabelEffectiveWidth = labelEffectiveWidth;

      this._labels[i].effectiveWidth = labelEffectiveWidth;
    }
  }

  var titleHeight = this.title
    ? getFontHeightInPixels(
        this.titleFontFamily,
        this.titleFontSize,
        this.titleFontWeight,
      ) + 2
    : 0;

  var axisWidth = titleHeight + maxLabelEffectiveWidth + this.tickLength + 5;

  return axisWidth;
};

Axis.prototype.createLabelsAndCalculateHeight = function () {
  var maxLabelEffectiveHeight = 0;
  this._labels = [];
  var textBlock;
  var i = 0;

  this.createLabels();

  if (this._position === "bottom" || this._position === "top") {
    for (i = 0; i < this._labels.length; i++) {
      textBlock = this._labels[i].textBlock;

      var size = textBlock.measureText();

      var labelEffectiveHeight = 0;

      if (this.labelAngle === 0) labelEffectiveHeight = size.height;
      else
        labelEffectiveHeight =
          size.width * Math.sin((Math.PI / 180) * Math.abs(this.labelAngle)) +
          (size.height / 2) *
            Math.cos((Math.PI / 180) * Math.abs(this.labelAngle));

      if (maxLabelEffectiveHeight < labelEffectiveHeight)
        maxLabelEffectiveHeight = labelEffectiveHeight;

      this._labels[i].effectiveHeight = labelEffectiveHeight;
    }
  }

  var titleHeight = this.title
    ? getFontHeightInPixels(
        this.titleFontFamily,
        this.titleFontSize,
        this.titleFontWeight,
      ) + 2
    : 0;

  return titleHeight + maxLabelEffectiveHeight + this.tickLength + 5;
};

Axis.setLayoutAndRender = function (
  axisX,
  axisY,
  axisY2,
  axisPlacement,
  freeSpace,
) {
  var x1, y1, x2, y2;
  var chart = axisX.chart;
  var ctx = chart.ctx;

  axisX.calculateAxisParameters();

  if (axisY) axisY.calculateAxisParameters();

  if (axisY2) axisY2.calculateAxisParameters();

  var axisYMargin = axisY ? axisY.margin : 0;

  if (axisPlacement === "normal") {
    axisX.lineCoordinates = {};

    var axisYWidth = Math.ceil(
      axisY ? axisY.createLabelsAndCalculateWidth() : 0,
    );
    x1 = Math.round(freeSpace.x1 + axisYWidth + axisYMargin);
    axisX.lineCoordinates.x1 = x1;

    var axisY2Width = Math.ceil(
      axisY2 ? axisY2.createLabelsAndCalculateWidth() : 0,
    );
    x2 = Math.round(
      freeSpace.x2 - axisY2Width > axisX.chart.width - 10
        ? axisX.chart.width - 10
        : freeSpace.x2 - axisY2Width,
    );
    axisX.lineCoordinates.x2 = x2;

    axisX.lineCoordinates.width = Math.abs(x2 - x1);

    var axisXHeight = Math.ceil(axisX.createLabelsAndCalculateHeight());

    y1 = Math.round(freeSpace.y2 - axisXHeight - axisX.margin);
    y2 = Math.round(freeSpace.y2 - axisX.margin);

    axisX.lineCoordinates.y1 = y1;
    axisX.lineCoordinates.y2 = y1;

    axisX.boundingRect = {
      x1: x1,
      y1: y1,
      x2: x2,
      y2: y2,
      width: x2 - x1,
      height: y2 - y1,
    };

    if (axisY) {
      x1 = Math.round(freeSpace.x1 + axisY.margin);
      y1 = Math.round(freeSpace.y1 < 10 ? 10 : freeSpace.y1);
      x2 = Math.round(freeSpace.x1 + axisYWidth + axisY.margin);
      y2 = Math.round(freeSpace.y2 - axisXHeight - axisX.margin);

      axisY.lineCoordinates = {
        x1: x2,
        y1: y1,
        x2: x2,
        y2: y2,
        height: Math.abs(y2 - y1),
      };

      axisY.boundingRect = {
        x1: x1,
        y1: y1,
        x2: x2,
        y2: y2,
        width: x2 - x1,
        height: y2 - y1,
      };
    }

    if (axisY2) {
      x1 = Math.round(axisX.lineCoordinates.x2);
      y1 = Math.round(freeSpace.y1 < 10 ? 10 : freeSpace.y1);
      x2 = Math.round(x1 + axisY2Width + axisY2.margin);
      y2 = Math.round(freeSpace.y2 - axisXHeight - axisX.margin);

      axisY2.lineCoordinates = {
        x1: x1,
        y1: y1,
        x2: x1,
        y2: y2,
        height: Math.abs(y2 - y1),
      };

      axisY2.boundingRect = {
        x1: x1,
        y1: y1,
        x2: x2,
        y2: y2,
        width: x2 - x1,
        height: y2 - y1,
      };
    }

    axisX.calculateValueToPixelConversionParameters();

    if (axisY) axisY.calculateValueToPixelConversionParameters();

    if (axisY2) axisY2.calculateValueToPixelConversionParameters();

    ctx.save();
    ctx.rect(
      5,
      axisX.boundingRect.y1,
      axisX.chart.width - 10,
      axisX.boundingRect.height,
    );
    ctx.clip();

    axisX.renderLabelsTicksAndTitle();
    ctx.restore();

    if (axisY) axisY.renderLabelsTicksAndTitle();

    if (axisY2) axisY2.renderLabelsTicksAndTitle();

    chart.preparePlotArea();
    var plotArea = axisX.chart.plotArea;

    ctx.save();

    ctx.rect(
      plotArea.x1,
      plotArea.y1,
      Math.abs(plotArea.x2 - plotArea.x1),
      Math.abs(plotArea.y2 - plotArea.y1),
    );

    ctx.clip();

    axisX.renderStripLinesOfThicknessType("value");

    if (axisY) axisY.renderStripLinesOfThicknessType("value");

    if (axisY2) axisY2.renderStripLinesOfThicknessType("value");

    axisX.renderInterlacedColors();

    if (axisY) axisY.renderInterlacedColors();

    if (axisY2) axisY2.renderInterlacedColors();

    ctx.restore();

    axisX.renderGrid();

    if (axisY) axisY.renderGrid();

    if (axisY2) axisY2.renderGrid();

    axisX.renderAxisLine();

    if (axisY) axisY.renderAxisLine();

    if (axisY2) axisY2.renderAxisLine();

    axisX.renderStripLinesOfThicknessType("pixel");

    if (axisY) axisY.renderStripLinesOfThicknessType("pixel");

    if (axisY2) axisY2.renderStripLinesOfThicknessType("pixel");
  } else {
    var axisXWidth = Math.ceil(axisX.createLabelsAndCalculateWidth());

    if (axisY) {
      axisY.lineCoordinates = {};

      x1 = Math.round(freeSpace.x1 + axisXWidth + axisX.margin);
      x2 = Math.round(
        freeSpace.x2 > axisY.chart.width - 10
          ? axisY.chart.width - 10
          : freeSpace.x2,
      );

      axisY.lineCoordinates.x1 = x1;
      axisY.lineCoordinates.x2 = x2;
      axisY.lineCoordinates.width = Math.abs(x2 - x1);
    }

    if (axisY2) {
      axisY2.lineCoordinates = {};
      x1 = Math.round(freeSpace.x1 + axisXWidth + axisX.margin);
      x2 = Math.round(
        freeSpace.x2 > axisY2.chart.width - 10
          ? axisY2.chart.width - 10
          : freeSpace.x2,
      );

      axisY2.lineCoordinates.x1 = x1;
      axisY2.lineCoordinates.x2 = x2;
      axisY2.lineCoordinates.width = Math.abs(x2 - x1);
    }

    var axisYHeight = Math.ceil(
      axisY ? axisY.createLabelsAndCalculateHeight() : 0,
    );
    var axisY2Height = Math.ceil(
      axisY2 ? axisY2.createLabelsAndCalculateHeight() : 0,
    );

    if (axisY) {
      y1 = Math.round(freeSpace.y2 - axisYHeight - axisY.margin);
      y2 = Math.round(
        freeSpace.y2 - axisYMargin > axisY.chart.height - 10
          ? axisY.chart.height - 10
          : freeSpace.y2 - axisYMargin,
      );

      axisY.lineCoordinates.y1 = y1;
      axisY.lineCoordinates.y2 = y1;

      axisY.boundingRect = {
        x1: x1,
        y1: y1,
        x2: x2,
        y2: y2,
        width: x2 - x1,
        height: axisYHeight,
      };
    }

    if (axisY2) {
      y1 = Math.round(freeSpace.y1 + axisY2.margin);
      y2 = freeSpace.y1 + axisY2.margin + axisY2Height;

      axisY2.lineCoordinates.y1 = y2;
      axisY2.lineCoordinates.y2 = y2;

      axisY2.boundingRect = {
        x1: x1,
        y1: y1,
        x2: x2,
        y2: y2,
        width: x2 - x1,
        height: axisY2Height,
      };
    }

    x1 = Math.round(freeSpace.x1 + axisX.margin);
    y1 = Math.round(
      axisY2
        ? axisY2.lineCoordinates.y2
        : freeSpace.y1 < 10
          ? 10
          : freeSpace.y1,
    );
    x2 = Math.round(freeSpace.x1 + axisXWidth + axisX.margin);
    y2 = Math.round(
      axisY
        ? axisY.lineCoordinates.y1
        : freeSpace.y2 - axisYMargin > axisX.chart.height - 10
          ? axisX.chart.height - 10
          : freeSpace.y2 - axisYMargin,
    );

    axisX.lineCoordinates = {
      x1: x2,
      y1: y1,
      x2: x2,
      y2: y2,
      height: Math.abs(y2 - y1),
    };

    axisX.boundingRect = {
      x1: x1,
      y1: y1,
      x2: x2,
      y2: y2,
      width: x2 - x1,
      height: y2 - y1,
    };

    axisX.calculateValueToPixelConversionParameters();

    if (axisY) axisY.calculateValueToPixelConversionParameters();
    if (axisY2) axisY2.calculateValueToPixelConversionParameters();

    if (axisY) axisY.renderLabelsTicksAndTitle();

    if (axisY2) axisY2.renderLabelsTicksAndTitle();

    axisX.renderLabelsTicksAndTitle();

    chart.preparePlotArea();
    var plotArea = axisX.chart.plotArea;

    ctx.save();
    ctx.rect(
      plotArea.x1,
      plotArea.y1,
      Math.abs(plotArea.x2 - plotArea.x1),
      Math.abs(plotArea.y2 - plotArea.y1),
    );

    ctx.clip();

    axisX.renderStripLinesOfThicknessType("value");

    if (axisY) axisY.renderStripLinesOfThicknessType("value");
    if (axisY2) axisY2.renderStripLinesOfThicknessType("value");

    axisX.renderInterlacedColors();

    if (axisY) axisY.renderInterlacedColors();
    if (axisY2) axisY2.renderInterlacedColors();

    ctx.restore();

    axisX.renderGrid();

    if (axisY) axisY.renderGrid();

    if (axisY2) axisY2.renderGrid();

    axisX.renderAxisLine();

    if (axisY) axisY.renderAxisLine();

    if (axisY2) axisY2.renderAxisLine();

    axisX.renderStripLinesOfThicknessType("pixel");

    if (axisY) axisY.renderStripLinesOfThicknessType("pixel");
    if (axisY2) axisY2.renderStripLinesOfThicknessType("pixel");
  }
};

Axis.prototype.renderLabelsTicksAndTitle = function () {
  var skipLabels = false;
  var totalLabelWidth = 0;
  var thresholdRatio = 1;
  var labelCount = 0;

  if (this.labelAngle !== 0 && this.labelAngle !== 360) thresholdRatio = 1.2;

  if (typeof this._options.interval === "undefined") {
    if (this._position === "bottom" || this._position === "top") {
      for (i = 0; i < this._labels.length; i++) {
        label = this._labels[i];
        if (label.position < this.viewportMinimum || label.stripLine) continue;

        var width =
          label.textBlock.width * Math.cos((Math.PI / 180) * this.labelAngle) +
          label.textBlock.height * Math.sin((Math.PI / 180) * this.labelAngle);

        totalLabelWidth += width;
      }

      if (totalLabelWidth > this.lineCoordinates.width * thresholdRatio) {
        skipLabels = true;
      }
    }
    if (this._position === "left" || this._position === "right") {
      for (i = 0; i < this._labels.length; i++) {
        label = this._labels[i];
        if (label.position < this.viewportMinimum || label.stripLine) continue;

        var width =
          label.textBlock.height * Math.cos((Math.PI / 180) * this.labelAngle) +
          label.textBlock.width * Math.sin((Math.PI / 180) * this.labelAngle);

        totalLabelWidth += width;
      }

      if (totalLabelWidth > this.lineCoordinates.height * thresholdRatio) {
        skipLabels = true;
      }
    }
  }

  if (this._position === "bottom") {
    var i = 0;

    var label;
    var xy;

    for (i = 0; i < this._labels.length; i++) {
      label = this._labels[i];
      if (
        label.position < this.viewportMinimum ||
        label.position > this.viewportMaximum
      )
        continue;

      xy = this.getPixelCoordinatesOnAxis(label.position);

      if (
        (this.tickThickness && !this._labels[i].stripLine) ||
        (this._labels[i].stripLine &&
          this._labels[i].stripLine._thicknessType === "pixel")
      ) {
        if (this._labels[i].stripLine) {
          stripLine = this._labels[i].stripLine;
          this.ctx.lineWidth = stripLine.thickness;
          this.ctx.strokeStyle = stripLine.color;
        } else {
          this.ctx.lineWidth = this.tickThickness;
          this.ctx.strokeStyle = this.tickColor;
        }

        var tickX =
          this.ctx.lineWidth % 2 === 1 ? (xy.x << 0) + 0.5 : xy.x << 0;
        this.ctx.beginPath();
        this.ctx.moveTo(tickX, xy.y << 0);
        this.ctx.lineTo(tickX, (xy.y + this.tickLength) << 0);
        this.ctx.stroke();
      }

      if (skipLabels && labelCount++ % 2 !== 0 && !this._labels[i].stripLine)
        continue;

      if (label.textBlock.angle === 0) {
        xy.x -= label.textBlock.width / 2;
        xy.y += this.tickLength + label.textBlock.fontSize / 2;
      } else {
        xy.x -=
          this.labelAngle < 0
            ? label.textBlock.width *
              Math.cos((Math.PI / 180) * this.labelAngle)
            : 0;
        xy.y +=
          this.tickLength +
          Math.abs(
            this.labelAngle < 0
              ? label.textBlock.width *
                  Math.sin((Math.PI / 180) * this.labelAngle) -
                  5
              : 5,
          );
      }
      label.textBlock.x = xy.x;
      label.textBlock.y = xy.y;

      label.textBlock.render(true);
    }

    if (this.title) {
      this._titleTextBlock = new TextBlock(this.ctx, {
        x: this.lineCoordinates.x1,
        y: this.boundingRect.y2 - this.titleFontSize - 5,
        maxWidth: this.lineCoordinates.width,
        maxHeight: this.titleFontSize * 1.5,
        angle: 0,
        text: this.title,
        horizontalAlign: "center",
        fontSize: this.titleFontSize,
        fontFamily: this.titleFontFamily,
        fontWeight: this.titleFontWeight,
        fontColor: this.titleFontColor,
        fontStyle: this.titleFontStyle,
        textBaseline: "top",
      });

      this._titleTextBlock.measureText();
      this._titleTextBlock.x =
        this.lineCoordinates.x1 +
        this.lineCoordinates.width / 2 -
        this._titleTextBlock.width / 2;
      this._titleTextBlock.y =
        this.boundingRect.y2 - this._titleTextBlock.height - 3;
      this._titleTextBlock.render(true);
    }
  } else if (this._position === "top") {
    var i = 0;

    var label;
    var xy;
    var stripLine;

    for (i = 0; i < this._labels.length; i++) {
      label = this._labels[i];
      if (
        label.position < this.viewportMinimum ||
        label.position > this.viewportMaximum
      )
        continue;

      xy = this.getPixelCoordinatesOnAxis(label.position);

      if (
        (this.tickThickness && !this._labels[i].stripLine) ||
        (this._labels[i].stripLine &&
          this._labels[i].stripLine._thicknessType === "pixel")
      ) {
        if (this._labels[i].stripLine) {
          stripLine = this._labels[i].stripLine;

          this.ctx.lineWidth = stripLine.thickness;
          this.ctx.strokeStyle = stripLine.color;
        } else {
          this.ctx.lineWidth = this.tickThickness;
          this.ctx.strokeStyle = this.tickColor;
        }

        var tickX =
          this.ctx.lineWidth % 2 === 1 ? (xy.x << 0) + 0.5 : xy.x << 0;
        this.ctx.beginPath();
        this.ctx.moveTo(tickX, xy.y << 0);
        this.ctx.lineTo(tickX, (xy.y - this.tickLength) << 0);
        this.ctx.stroke();
      }

      if (skipLabels && labelCount++ % 2 !== 0 && !this._labels[i].stripLine)
        continue;

      if (label.textBlock.angle === 0) {
        xy.x -= label.textBlock.width / 2;
        xy.y -= this.tickLength + label.textBlock.height / 2;
      } else {
        xy.x -=
          this.labelAngle > 0
            ? label.textBlock.width *
              Math.cos((Math.PI / 180) * this.labelAngle)
            : 0;
        xy.y -=
          this.tickLength +
          Math.abs(
            this.labelAngle > 0
              ? label.textBlock.width *
                  Math.sin((Math.PI / 180) * this.labelAngle) +
                  5
              : 5,
          );
      }
      label.textBlock.x = xy.x;
      label.textBlock.y = xy.y;

      label.textBlock.render(true);
    }

    if (this.title) {
      this._titleTextBlock = new TextBlock(this.ctx, {
        x: this.lineCoordinates.x1,
        y: this.boundingRect.y1 + 1,
        maxWidth: this.lineCoordinates.width,
        maxHeight: this.titleFontSize * 1.5,
        angle: 0,
        text: this.title,
        horizontalAlign: "center",
        fontSize: this.titleFontSize,
        fontFamily: this.titleFontFamily,
        fontWeight: this.titleFontWeight,
        fontColor: this.titleFontColor,
        fontStyle: this.titleFontStyle,
        textBaseline: "top",
      });

      this._titleTextBlock.measureText();
      this._titleTextBlock.x =
        this.lineCoordinates.x1 +
        this.lineCoordinates.width / 2 -
        this._titleTextBlock.width / 2;
      this._titleTextBlock.render(true);
    }
  } else if (this._position === "left") {
    var label;
    var xy;
    for (var i = 0; i < this._labels.length; i++) {
      label = this._labels[i];
      if (
        label.position < this.viewportMinimum ||
        label.position > this.viewportMaximum
      )
        continue;

      xy = this.getPixelCoordinatesOnAxis(label.position);

      if (
        (this.tickThickness && !this._labels[i].stripLine) ||
        (this._labels[i].stripLine &&
          this._labels[i].stripLine._thicknessType === "pixel")
      ) {
        if (this._labels[i].stripLine) {
          stripLine = this._labels[i].stripLine;

          this.ctx.lineWidth = stripLine.thickness;
          this.ctx.strokeStyle = stripLine.color;
        } else {
          this.ctx.lineWidth = this.tickThickness;
          this.ctx.strokeStyle = this.tickColor;
        }

        var tickY =
          this.ctx.lineWidth % 2 === 1 ? (xy.y << 0) + 0.5 : xy.y << 0;
        this.ctx.beginPath();
        this.ctx.moveTo(xy.x << 0, tickY);
        this.ctx.lineTo((xy.x - this.tickLength) << 0, tickY);
        this.ctx.stroke();
      }

      if (skipLabels && labelCount++ % 2 !== 0 && !this._labels[i].stripLine)
        continue;

      label.textBlock.x =
        xy.x -
        label.textBlock.width * Math.cos((Math.PI / 180) * this.labelAngle) -
        this.tickLength -
        5;

      if (this.labelAngle === 0) {
        label.textBlock.y = xy.y;
      } else
        label.textBlock.y =
          xy.y -
          label.textBlock.width * Math.sin((Math.PI / 180) * this.labelAngle);

      label.textBlock.render(true);
    }

    if (this.title) {
      this._titleTextBlock = new TextBlock(this.ctx, {
        x: this.boundingRect.x1 + 1,
        y: this.lineCoordinates.y2,
        maxWidth: this.lineCoordinates.height,
        maxHeight: this.titleFontSize * 1.5,
        angle: -90,
        text: this.title,
        horizontalAlign: "center",
        fontSize: this.titleFontSize,
        fontFamily: this.titleFontFamily,
        fontWeight: this.titleFontWeight,
        fontColor: this.titleFontColor,
        fontStyle: this.titleFontStyle,
        textBaseline: "top",
      });

      this._titleTextBlock.y =
        this.lineCoordinates.height / 2 +
        this._titleTextBlock.width / 2 +
        this.lineCoordinates.y1;
      this._titleTextBlock.render(true);
    }
  } else if (this._position === "right") {
    var label;
    var xy;

    for (var i = 0; i < this._labels.length; i++) {
      label = this._labels[i];
      if (
        label.position < this.viewportMinimum ||
        label.position > this.viewportMaximum
      )
        continue;

      xy = this.getPixelCoordinatesOnAxis(label.position);

      if (
        (this.tickThickness && !this._labels[i].stripLine) ||
        (this._labels[i].stripLine &&
          this._labels[i].stripLine._thicknessType === "pixel")
      ) {
        if (this._labels[i].stripLine) {
          stripLine = this._labels[i].stripLine;

          this.ctx.lineWidth = stripLine.thickness;
          this.ctx.strokeStyle = stripLine.color;
        } else {
          this.ctx.lineWidth = this.tickThickness;
          this.ctx.strokeStyle = this.tickColor;
        }

        var tickY =
          this.ctx.lineWidth % 2 === 1 ? (xy.y << 0) + 0.5 : xy.y << 0;
        this.ctx.beginPath();
        this.ctx.moveTo(xy.x << 0, tickY);
        this.ctx.lineTo((xy.x + this.tickLength) << 0, tickY);
        this.ctx.stroke();
      }

      if (skipLabels && labelCount++ % 2 !== 0 && !this._labels[i].stripLine)
        continue;

      label.textBlock.x = xy.x + this.tickLength + 5;
      if (this.labelAngle === 0) {
        label.textBlock.y = xy.y;
      } else label.textBlock.y = xy.y;

      label.textBlock.render(true);
    }

    if (this.title) {
      this._titleTextBlock = new TextBlock(this.ctx, {
        x: this.boundingRect.x2 - 1,
        y: this.lineCoordinates.y2,
        maxWidth: this.lineCoordinates.height,
        maxHeight: this.titleFontSize * 1.5,
        angle: 90,
        text: this.title,
        horizontalAlign: "center",
        fontSize: this.titleFontSize,
        fontFamily: this.titleFontFamily,
        fontWeight: this.titleFontWeight,
        fontColor: this.titleFontColor,
        fontStyle: this.titleFontStyle,
        textBaseline: "top",
      });

      this._titleTextBlock.measureText();
      this._titleTextBlock.y =
        this.lineCoordinates.height / 2 -
        this._titleTextBlock.width / 2 +
        this.lineCoordinates.y1;
      this._titleTextBlock.render(true);
    }
  }
};

Axis.prototype.renderInterlacedColors = function () {
  var ctx = this.chart.plotArea.ctx;

  var interlacedGridStartPoint;
  var interlacedGridEndPoint;
  var plotAreaCoordinates = this.chart.plotArea;
  var i = 0,
    renderInterlacedGrid = true;

  if (
    (this._position === "bottom" || this._position === "top") &&
    this.interlacedColor
  ) {
    ctx.fillStyle = this.interlacedColor;

    for (i = 0; i < this._labels.length; i++) {
      if (this._labels[i].stripLine) continue;

      if (renderInterlacedGrid) {
        interlacedGridStartPoint = this.getPixelCoordinatesOnAxis(
          this._labels[i].position,
        );

        if (i + 1 >= this._labels.length - 1)
          interlacedGridEndPoint = this.getPixelCoordinatesOnAxis(
            this.viewportMaximum,
          );
        else
          interlacedGridEndPoint = this.getPixelCoordinatesOnAxis(
            this._labels[i + 1].position,
          );

        ctx.fillRect(
          interlacedGridStartPoint.x,
          plotAreaCoordinates.y1,
          Math.abs(interlacedGridEndPoint.x - interlacedGridStartPoint.x),
          Math.abs(plotAreaCoordinates.y1 - plotAreaCoordinates.y2),
        );
        renderInterlacedGrid = false;
      } else renderInterlacedGrid = true;
    }
  } else if (
    (this._position === "left" || this._position === "right") &&
    this.interlacedColor
  ) {
    ctx.fillStyle = this.interlacedColor;

    for (i = 0; i < this._labels.length; i++) {
      if (this._labels[i].stripLine) continue;

      if (renderInterlacedGrid) {
        interlacedGridEndPoint = this.getPixelCoordinatesOnAxis(
          this._labels[i].position,
        );

        if (i + 1 >= this._labels.length - 1)
          interlacedGridStartPoint = this.getPixelCoordinatesOnAxis(
            this.viewportMaximum,
          );
        else
          interlacedGridStartPoint = this.getPixelCoordinatesOnAxis(
            this._labels[i + 1].position,
          );

        ctx.fillRect(
          plotAreaCoordinates.x1,
          interlacedGridStartPoint.y,
          Math.abs(plotAreaCoordinates.x1 - plotAreaCoordinates.x2),
          Math.abs(interlacedGridStartPoint.y - interlacedGridEndPoint.y),
        );
        renderInterlacedGrid = false;
      } else renderInterlacedGrid = true;
    }
  }

  ctx.beginPath();
};

Axis.prototype.renderStripLinesOfThicknessType = function (thicknessType) {
  if (!(this.stripLines && this.stripLines.length > 0) || !thicknessType)
    return;

  var i = 0;
  for (i = 0; i < this.stripLines.length; i++) {
    var stripLine = this.stripLines[i];

    if (stripLine._thicknessType !== thicknessType) continue;

    if (
      thicknessType === "pixel" &&
      (stripLine.value < this.viewportMinimum ||
        stripLine.value > this.viewportMaximum)
    )
      continue;

    if (stripLine.showOnTop) {
      this.chart.addEventListener(
        "dataAnimationIterationEnd",
        stripLine.render,
        stripLine,
      );
    } else stripLine.render();
  }
};

Axis.prototype.renderGrid = function () {
  if (!(this.gridThickness && this.gridThickness > 0)) return;

  var ctx = this.chart.ctx;

  var xy;
  var plotAreaCoordinates = this.chart.plotArea;

  ctx.lineWidth = this.gridThickness;
  ctx.strokeStyle = this.gridColor;

  if (ctx.setLineDash) {
    ctx.setLineDash(getLineDashArray(this.gridDashType, this.gridThickness));
  }

  if (this._position === "bottom" || this._position === "top") {
    for (i = 0; i < this._labels.length && !this._labels[i].stripLine; i++) {
      if (
        this._labels[i].position < this.viewportMinimum ||
        this._labels[i].position > this.viewportMaximum
      )
        continue;

      ctx.beginPath();

      xy = this.getPixelCoordinatesOnAxis(this._labels[i].position);

      var gridX = ctx.lineWidth % 2 === 1 ? (xy.x << 0) + 0.5 : xy.x << 0;

      ctx.moveTo(gridX, plotAreaCoordinates.y1 << 0);
      ctx.lineTo(gridX, plotAreaCoordinates.y2 << 0);

      ctx.stroke();
    }
  } else if (this._position === "left" || this._position === "right") {
    for (
      var i = 0;
      i < this._labels.length && !this._labels[i].stripLine;
      i++
    ) {
      if (
        i === 0 &&
        this.type === "axisY" &&
        this.chart.axisX &&
        this.chart.axisX.lineThickness
      )
        continue;

      if (
        this._labels[i].position < this.viewportMinimum ||
        this._labels[i].position > this.viewportMaximum
      )
        continue;

      ctx.beginPath();

      xy = this.getPixelCoordinatesOnAxis(this._labels[i].position);

      var gridY = ctx.lineWidth % 2 === 1 ? (xy.y << 0) + 0.5 : xy.y << 0;

      ctx.moveTo(plotAreaCoordinates.x1 << 0, gridY);
      ctx.lineTo(plotAreaCoordinates.x2 << 0, gridY);

      ctx.stroke();
    }
  }
};

Axis.prototype.renderAxisLine = function () {
  var ctx = this.chart.ctx;

  if (this._position === "bottom" || this._position === "top") {
    if (this.lineThickness) {
      ctx.lineWidth = this.lineThickness;
      ctx.strokeStyle = this.lineColor ? this.lineColor : "black";

      if (ctx.setLineDash) {
        ctx.setLineDash(
          getLineDashArray(this.lineDashType, this.lineThickness),
        );
      }

      var lineY =
        this.lineThickness % 2 === 1
          ? (this.lineCoordinates.y1 << 0) + 0.5
          : this.lineCoordinates.y1 << 0;

      ctx.beginPath();
      ctx.moveTo(this.lineCoordinates.x1, lineY);
      ctx.lineTo(this.lineCoordinates.x2, lineY);
      ctx.stroke();
    }
  } else if (this._position === "left" || this._position === "right") {
    if (this.lineThickness) {
      ctx.lineWidth = this.lineThickness;
      ctx.strokeStyle = this.lineColor;

      if (ctx.setLineDash) {
        ctx.setLineDash(
          getLineDashArray(this.lineDashType, this.lineThickness),
        );
      }

      var lineX =
        this.lineThickness % 2 === 1
          ? (this.lineCoordinates.x1 << 0) + 0.5
          : this.lineCoordinates.x1 << 0;

      ctx.beginPath();
      ctx.moveTo(lineX, this.lineCoordinates.y1);
      ctx.lineTo(lineX, this.lineCoordinates.y2);
      ctx.stroke();
    }
  }
};

Axis.prototype.getPixelCoordinatesOnAxis = function (value) {
  var xy = {};

  if (this._position === "bottom" || this._position === "top") {
    var pixelPerUnit = this.conversionParameters.pixelPerUnit;

    xy.x =
      this.conversionParameters.reference +
      pixelPerUnit * (value - this.viewportMinimum);
    xy.y = this.lineCoordinates.y1;
  }
  if (this._position === "left" || this._position === "right") {
    var pixelPerUnit = -this.conversionParameters.pixelPerUnit;

    xy.y =
      this.conversionParameters.reference -
      pixelPerUnit * (value - this.viewportMinimum);
    xy.x = this.lineCoordinates.x2;
  }

  return xy;
};

Axis.prototype.convertPixelToValue = function (pixel) {
  if (!pixel) return null;

  var value = 0;
  var p =
    this._position === "left" || this._position === "right" ? pixel.y : pixel.x;

  value =
    this.conversionParameters.minimum +
    (p - this.conversionParameters.reference) /
      this.conversionParameters.pixelPerUnit;

  return value;
};

Axis.prototype.setViewPortRange = function (viewportMinimum, viewportMaximum) {
  this.sessionVariables.newViewportMinimum = this.viewportMinimum = Math.min(
    viewportMinimum,
    viewportMaximum,
  );
  this.sessionVariables.newViewportMaximum = this.viewportMaximum = Math.max(
    viewportMinimum,
    viewportMaximum,
  );
};

Axis.prototype.getXValueAt = function (pixel) {
  if (!pixel) return null;

  var xval = null;

  if (this._position === "left") {
    xval =
      ((this.chart.axisX.viewportMaximum - this.chart.axisX.viewportMinimum) /
        this.chart.axisX.lineCoordinates.height) *
        (this.chart.axisX.lineCoordinates.y2 - pixel.y) +
      this.chart.axisX.viewportMinimum;
  } else if (this._position === "bottom") {
    xval =
      ((this.chart.axisX.viewportMaximum - this.chart.axisX.viewportMinimum) /
        this.chart.axisX.lineCoordinates.width) *
        (pixel.x - this.chart.axisX.lineCoordinates.x1) +
      this.chart.axisX.viewportMinimum;
  }

  return xval;
};

Axis.prototype.calculateValueToPixelConversionParameters = function () {
  this.reversed = false;

  var conversionParameters = {
    pixelPerUnit: null,
    minimum: null,
    reference: null,
  };

  var width = this.lineCoordinates.width;
  var height = this.lineCoordinates.height;

  conversionParameters.minimum = this.viewportMinimum;

  if (this._position === "bottom" || this._position === "top") {
    conversionParameters.pixelPerUnit =
      ((this.reversed ? -1 : 1) * width) /
      Math.abs(this.viewportMaximum - this.viewportMinimum);
    conversionParameters.reference = this.reversed
      ? this.lineCoordinates.x2
      : this.lineCoordinates.x1;
  }

  if (this._position === "left" || this._position === "right") {
    conversionParameters.pixelPerUnit =
      ((this.reversed ? 1 : -1) * height) /
      Math.abs(this.viewportMaximum - this.viewportMinimum);
    conversionParameters.reference = this.reversed
      ? this.lineCoordinates.y1
      : this.lineCoordinates.y2;
  }

  this.conversionParameters = conversionParameters;
};

Axis.prototype.calculateAxisParameters = function () {
  var freeSpace = this.chart.layoutManager.getFreeSpace();
  var isLessThanTwoDataPoints = false;

  if (this._position === "bottom" || this._position === "top") {
    this.maxWidth = freeSpace.width;
    this.maxHeight = freeSpace.height;
  } else {
    this.maxWidth = freeSpace.height;
    this.maxHeight = freeSpace.width;
  }

  var noTicks =
    this.type === "axisX"
      ? this.maxWidth < 500
        ? 8
        : Math.max(6, Math.floor(this.maxWidth / 62))
      : Math.max(Math.floor(this.maxWidth / 40), 2);
  var min, max;
  var minDiff;
  var range;
  var rangePadding = 0;

  if (this.viewportMinimum === null || isNaN(this.viewportMinimum))
    this.viewportMinimum = this.minimum;

  if (this.viewportMaximum === null || isNaN(this.viewportMaximum))
    this.viewportMaximum = this.maximum;

  if (this.type === "axisX") {
    min =
      this.viewportMinimum !== null
        ? this.viewportMinimum
        : this.dataInfo.viewPortMin;
    max =
      this.viewportMaximum !== null
        ? this.viewportMaximum
        : this.dataInfo.viewPortMax;

    if (max - min === 0) {
      rangePadding =
        typeof this._options.interval === "undefined"
          ? 0.4
          : this._options.interval;

      max += rangePadding;
      min -= rangePadding;
    }

    if (this.dataInfo.minDiff !== Infinity) minDiff = this.dataInfo.minDiff;
    else if (max - min > 1) {
      minDiff = Math.abs(max - min) * 0.5;
    } else {
      minDiff = 1;

      if (this.chart.plotInfo.axisXValueType === "dateTime")
        isLessThanTwoDataPoints = true;
    }
  } else if (this.type === "axisY") {
    min =
      this.viewportMinimum !== null
        ? this.viewportMinimum
        : this.dataInfo.viewPortMin;
    max =
      this.viewportMaximum !== null
        ? this.viewportMaximum
        : this.dataInfo.viewPortMax;

    if (!isFinite(min) && !isFinite(max)) {
      max =
        typeof this._options.interval === "undefined"
          ? -Infinity
          : this._options.interval;
      min = 0;
    } else if (!isFinite(min)) {
      min = max;
    } else if (!isFinite(max)) {
      max = min;
    }

    if (min === 0 && max === 0) {
      max += 9;
      min = 0;
    } else if (max - min === 0) {
      rangePadding = Math.min(Math.abs(Math.abs(max) * 0.01), 5);
      max += rangePadding;
      min -= rangePadding;
    } else if (min > max) {
      rangePadding = Math.min(Math.abs(Math.abs(max - min) * 0.01), 5);

      if (max >= 0) min = max - rangePadding;
      else max = min + rangePadding;
    } else {
      rangePadding = Math.min(Math.abs(Math.abs(max - min) * 0.01), 0.05);

      if (max !== 0) max += rangePadding;
      if (min !== 0) min -= rangePadding;
    }

    if (this.dataInfo.minDiff !== Infinity) minDiff = this.dataInfo.minDiff;
    else if (max - min > 1) {
      minDiff = Math.abs(max - min) * 0.5;
    } else {
      minDiff = 1;
    }

    if (
      this.includeZero &&
      (this.viewportMinimum === null || isNaN(this.viewportMinimum))
    ) {
      if (min > 0) min = 0;
    }
    if (
      this.includeZero &&
      (this.viewportMaximum === null || isNaN(this.viewportMaximum))
    ) {
      if (max < 0) max = 0;
    }
  }

  range =
    (isNaN(this.viewportMaximum) || this.viewportMaximum === null
      ? max
      : this.viewportMaximum) -
    (isNaN(this.viewportMinimum) || this.viewportMinimum === null
      ? min
      : this.viewportMinimum);

  if (
    this.type === "axisX" &&
    this.chart.plotInfo.axisXValueType === "dateTime"
  ) {
    if (!this.intervalType) {
      if (range / (1 * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "millisecond";
      } else if (range / (1 * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "millisecond";
      } else if (range / (1 * 5) <= noTicks) {
        this.interval = 5;
        this.intervalType = "millisecond";
      } else if (range / (1 * 10) <= noTicks) {
        this.interval = 10;
        this.intervalType = "millisecond";
      } else if (range / (1 * 20) <= noTicks) {
        this.interval = 20;
        this.intervalType = "millisecond";
      } else if (range / (1 * 50) <= noTicks) {
        this.interval = 50;
        this.intervalType = "millisecond";
      } else if (range / (1 * 100) <= noTicks) {
        this.interval = 100;
        this.intervalType = "millisecond";
      } else if (range / (1 * 200) <= noTicks) {
        this.interval = 200;
        this.intervalType = "millisecond";
      } else if (range / (1 * 250) <= noTicks) {
        this.interval = 250;
        this.intervalType = "millisecond";
      } else if (range / (1 * 300) <= noTicks) {
        this.interval = 300;
        this.intervalType = "millisecond";
      } else if (range / (1 * 400) <= noTicks) {
        this.interval = 400;
        this.intervalType = "millisecond";
      } else if (range / (1 * 500) <= noTicks) {
        this.interval = 500;
        this.intervalType = "millisecond";
      } else if (range / (constants.secondDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "second";
      } else if (range / (constants.secondDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "second";
      } else if (range / (constants.secondDuration * 5) <= noTicks) {
        this.interval = 5;
        this.intervalType = "second";
      } else if (range / (constants.secondDuration * 10) <= noTicks) {
        this.interval = 10;
        this.intervalType = "second";
      } else if (range / (constants.secondDuration * 15) <= noTicks) {
        this.interval = 15;
        this.intervalType = "second";
      } else if (range / (constants.secondDuration * 20) <= noTicks) {
        this.interval = 20;
        this.intervalType = "second";
      } else if (range / (constants.secondDuration * 30) <= noTicks) {
        this.interval = 30;
        this.intervalType = "second";
      } else if (range / (constants.minuteDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "minute";
      } else if (range / (constants.minuteDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "minute";
      } else if (range / (constants.minuteDuration * 5) <= noTicks) {
        this.interval = 5;
        this.intervalType = "minute";
      } else if (range / (constants.minuteDuration * 10) <= noTicks) {
        this.interval = 10;
        this.intervalType = "minute";
      } else if (range / (constants.minuteDuration * 15) <= noTicks) {
        this.interval = 15;
        this.intervalType = "minute";
      } else if (range / (constants.minuteDuration * 20) <= noTicks) {
        this.interval = 20;
        this.intervalType = "minute";
      } else if (range / (constants.minuteDuration * 30) <= noTicks) {
        this.interval = 30;
        this.intervalType = "minute";
      } else if (range / (constants.hourDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "hour";
      } else if (range / (constants.hourDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "hour";
      } else if (range / (constants.hourDuration * 3) <= noTicks) {
        this.interval = 3;
        this.intervalType = "hour";
      } else if (range / (constants.hourDuration * 6) <= noTicks) {
        this.interval = 6;
        this.intervalType = "hour";
      } else if (range / (constants.dayDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "day";
      } else if (range / (constants.dayDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "day";
      } else if (range / (constants.dayDuration * 4) <= noTicks) {
        this.interval = 4;
        this.intervalType = "day";
      } else if (range / (constants.weekDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "week";
      } else if (range / (constants.weekDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "week";
      } else if (range / (constants.weekDuration * 3) <= noTicks) {
        this.interval = 3;
        this.intervalType = "week";
      } else if (range / (constants.monthDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "month";
      } else if (range / (constants.monthDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "month";
      } else if (range / (constants.monthDuration * 3) <= noTicks) {
        this.interval = 3;
        this.intervalType = "month";
      } else if (range / (constants.monthDuration * 6) <= noTicks) {
        this.interval = 6;
        this.intervalType = "month";
      } else if (range / (constants.yearDuration * 1) <= noTicks) {
        this.interval = 1;
        this.intervalType = "year";
      } else if (range / (constants.yearDuration * 2) <= noTicks) {
        this.interval = 2;
        this.intervalType = "year";
      } else if (range / (constants.yearDuration * 4) <= noTicks) {
        this.interval = 4;
        this.intervalType = "year";
      } else {
        this.interval = Math.floor(
          Axis.getNiceNumber(range / (noTicks - 1), true) /
            constants.yearDuration,
        );
        this.intervalType = "year";
      }
    }

    if (this.viewportMinimum === null || isNaN(this.viewportMinimum))
      this.viewportMinimum = min - minDiff / 2;

    if (this.viewportMaximum === null || isNaN(this.viewportMaximum))
      this.viewportMaximum = max + minDiff / 2;

    if (!this.valueFormatString) {
      if (isLessThanTwoDataPoints) {
        this.valueFormatString = "MMM DD YYYY HH:mm";
      } else if (this.intervalType === "year") {
        this.valueFormatString = "YYYY";
      } else if (this.intervalType === "month") {
        this.valueFormatString = "MMM YYYY";
      } else if (this.intervalType === "week") {
        this.valueFormatString = "MMM DD YYYY";
      } else if (this.intervalType === "day") {
        this.valueFormatString = "MMM DD YYYY";
      } else if (this.intervalType === "hour") {
        this.valueFormatString = "hh:mm TT";
      } else if (this.intervalType === "minute") {
        this.valueFormatString = "hh:mm TT";
      } else if (this.intervalType === "second") {
        this.valueFormatString = "hh:mm:ss TT";
      } else if (this.intervalType === "millisecond") {
        this.valueFormatString = "fff'ms'";
      }
    }
  } else {
    this.intervalType = "number";

    range = Axis.getNiceNumber(range, false);

    if (this._options && this._options.interval)
      this.interval = this._options.interval;
    else {
      this.interval = Axis.getNiceNumber(range / (noTicks - 1), true);
    }

    if (this.viewportMinimum === null || isNaN(this.viewportMinimum)) {
      if (this.type === "axisX") this.viewportMinimum = min - minDiff / 2;
      else
        this.viewportMinimum = Math.floor(min / this.interval) * this.interval;
    }

    if (this.viewportMaximum === null || isNaN(this.viewportMaximum)) {
      if (this.type === "axisX") this.viewportMaximum = max + minDiff / 2;
      else
        this.viewportMaximum = Math.ceil(max / this.interval) * this.interval;
    }

    if (this.viewportMaximum === 0 && this.viewportMinimum === 0) {
      if (this._options.viewportMinimum === 0) {
        this.viewportMaximum += 10;
      } else if (this._options.viewportMaximum === 0) {
        this.viewportMinimum -= 10;
      }

      if (this._options && typeof this._options.interval === "undefined") {
        this.interval = Axis.getNiceNumber(
          (this.viewportMaximum - this.viewportMinimum) / (noTicks - 1),
          true,
        );
      }
    }
  }

  if (this.minimum === null || this.maximum === null) {
    if (this.type === "axisX") {
      min = this.minimum !== null ? this.minimum : this.dataInfo.min;
      max = this.maximum !== null ? this.maximum : this.dataInfo.max;

      if (max - min === 0) {
        rangePadding =
          typeof this._options.interval === "undefined"
            ? 0.4
            : this._options.interval;

        max += rangePadding;
        min -= rangePadding;
      }

      if (this.dataInfo.minDiff !== Infinity) minDiff = this.dataInfo.minDiff;
      else if (max - min > 1) {
        minDiff = Math.abs(max - min) * 0.5;
      } else {
        minDiff = 1;
      }
    } else if (this.type === "axisY") {
      min = this.minimum !== null ? this.minimum : this.dataInfo.min;
      max = this.maximum !== null ? this.maximum : this.dataInfo.max;

      if (!isFinite(min) && !isFinite(max)) {
        max =
          typeof this._options.interval === "undefined"
            ? -Infinity
            : this._options.interval;
        min = 0;
      } else if (min === 0 && max === 0) {
        max += 9;
        min = 0;
      } else if (max - min === 0) {
        rangePadding = Math.min(Math.abs(Math.abs(max) * 0.01), 5);
        max += rangePadding;
        min -= rangePadding;
      } else if (min > max) {
        rangePadding = Math.min(Math.abs(Math.abs(max - min) * 0.01), 5);

        if (max >= 0) min = max - rangePadding;
        else max = min + rangePadding;
      } else {
        rangePadding = Math.min(Math.abs(Math.abs(max - min) * 0.01), 0.05);

        if (max !== 0) max += rangePadding;
        if (min !== 0) min -= rangePadding;
      }

      if (this.dataInfo.minDiff !== Infinity) minDiff = this.dataInfo.minDiff;
      else if (max - min > 1) {
        minDiff = Math.abs(max - min) * 0.5;
      } else {
        minDiff = 1;
      }

      if (this.includeZero && (this.minimum === null || isNaN(this.minimum))) {
        if (min > 0) min = 0;
      }
      if (this.includeZero && (this.maximum === null || isNaN(this.maximum))) {
        if (max < 0) max = 0;
      }
    }

    range = max - min;

    if (
      this.type === "axisX" &&
      this.chart.plotInfo.axisXValueType === "dateTime"
    ) {
      if (this.minimum === null || isNaN(this.minimum))
        this.minimum = min - minDiff / 2;

      if (this.maximum === null || isNaN(this.maximum))
        this.maximum = max + minDiff / 2;
    } else {
      this.intervalType = "number";

      if (this.minimum === null) {
        if (this.type === "axisX") this.minimum = min - minDiff / 2;
        else this.minimum = Math.floor(min / this.interval) * this.interval;

        this.minimum = Math.min(
          this.minimum,
          this.sessionVariables.viewportMinimum === null ||
            isNaN(this.sessionVariables.viewportMinimum)
            ? Infinity
            : this.sessionVariables.viewportMinimum,
        );
      }

      if (this.maximum === null) {
        if (this.type === "axisX") this.maximum = max + minDiff / 2;
        else this.maximum = Math.ceil(max / this.interval) * this.interval;

        this.maximum = Math.max(
          this.maximum,
          this.sessionVariables.viewportMaximum === null ||
            isNaN(this.sessionVariables.viewportMaximum)
            ? -Infinity
            : this.sessionVariables.viewportMaximum,
        );
      }

      if (this.maximum === 0 && this.minimum === 0) {
        if (this._options.minimum === 0) {
          this.maximum += 10;
        } else if (this._options.maximum === 0) {
          this.minimum -= 10;
        }
      }
    }
  }

  this.viewportMinimum = Math.max(this.viewportMinimum, this.minimum);
  this.viewportMaximum = Math.min(this.viewportMaximum, this.maximum);

  if (
    this.type === "axisX" &&
    this.chart.plotInfo.axisXValueType === "dateTime"
  )
    this.intervalStartPosition = this.getLabelStartPoint(
      new Date(this.viewportMinimum),
      this.intervalType,
      this.interval,
    );
  else
    this.intervalStartPosition =
      Math.floor((this.viewportMinimum + this.interval * 0.2) / this.interval) *
      this.interval;

  if (!this.valueFormatString) {
    this.valueFormatString = "#,##0.##";

    range = Math.abs(this.viewportMaximum - this.viewportMinimum);

    if (range < 1) {
      var numberOfDecimals =
        Math.floor(Math.abs(Math.log(range) / Math.LN10)) + 2;

      if (isNaN(numberOfDecimals) || !isFinite(numberOfDecimals))
        numberOfDecimals = 2;

      if (numberOfDecimals > 2) {
        for (var i = 0; i < numberOfDecimals - 2; i++)
          this.valueFormatString += "#";
      }
    }
  }
};

Axis.getNiceNumber = function (x, round) {
  var exp = Math.floor(Math.log(x) / Math.LN10);
  var f = x / Math.pow(10, exp);
  var nf;

  if (round) {
    if (f < 1.5) nf = 1;
    else if (f < 3) nf = 2;
    else if (f < 7) nf = 5;
    else nf = 10;
  } else {
    if (f <= 1) nf = 1;
    else if (f <= 2) nf = 2;
    else if (f <= 5) nf = 5;
    else nf = 10;
  }

  return Number((nf * Math.pow(10, exp)).toFixed(20));
};

Axis.prototype.getLabelStartPoint = function () {
  var intervalInMilliseconds = convertToNumber(
    this.interval,
    this.intervalType,
  );
  var minimum =
    Math.floor(this.viewportMinimum / intervalInMilliseconds) *
    intervalInMilliseconds;
  var dateTime = new Date(minimum);

  if (this.intervalType === "millisecond") {
  } else if (this.intervalType === "second") {
    if (dateTime.getMilliseconds() > 0) {
      dateTime.setSeconds(dateTime.getSeconds() + 1);
      dateTime.setMilliseconds(0);
    }
  } else if (this.intervalType === "minute") {
    if (dateTime.getSeconds() > 0 || dateTime.getMilliseconds() > 0) {
      dateTime.setMinutes(dateTime.getMinutes() + 1);
      dateTime.setSeconds(0);
      dateTime.setMilliseconds(0);
    }
  } else if (this.intervalType === "hour") {
    if (
      dateTime.getMinutes() > 0 ||
      dateTime.getSeconds() > 0 ||
      dateTime.getMilliseconds() > 0
    ) {
      dateTime.setHours(dateTime.getHours() + 1);
      dateTime.setMinutes(0);
      dateTime.setSeconds(0);
      dateTime.setMilliseconds(0);
    }
  } else if (this.intervalType === "day") {
    if (
      dateTime.getHours() > 0 ||
      dateTime.getMinutes() > 0 ||
      dateTime.getSeconds() > 0 ||
      dateTime.getMilliseconds() > 0
    ) {
      dateTime.setDate(dateTime.getDate() + 1);
      dateTime.setHours(0);
      dateTime.setMinutes(0);
      dateTime.setSeconds(0);
      dateTime.setMilliseconds(0);
    }
  } else if (this.intervalType === "week") {
    if (
      dateTime.getDay() > 0 ||
      dateTime.getHours() > 0 ||
      dateTime.getMinutes() > 0 ||
      dateTime.getSeconds() > 0 ||
      dateTime.getMilliseconds() > 0
    ) {
      dateTime.setDate(dateTime.getDate() + (7 - dateTime.getDay()));
      dateTime.setHours(0);
      dateTime.setMinutes(0);
      dateTime.setSeconds(0);
      dateTime.setMilliseconds(0);
    }
  } else if (this.intervalType === "month") {
    if (
      dateTime.getDate() > 1 ||
      dateTime.getHours() > 0 ||
      dateTime.getMinutes() > 0 ||
      dateTime.getSeconds() > 0 ||
      dateTime.getMilliseconds() > 0
    ) {
      dateTime.setMonth(dateTime.getMonth() + 1);
      dateTime.setDate(1);
      dateTime.setHours(0);
      dateTime.setMinutes(0);
      dateTime.setSeconds(0);
      dateTime.setMilliseconds(0);
    }
  } else if (this.intervalType === "year") {
    if (
      dateTime.getMonth() > 0 ||
      dateTime.getDate() > 1 ||
      dateTime.getHours() > 0 ||
      dateTime.getMinutes() > 0 ||
      dateTime.getSeconds() > 0 ||
      dateTime.getMilliseconds() > 0
    ) {
      dateTime.setFullYear(dateTime.getFullYear() + 1);
      dateTime.setMonth(0);
      dateTime.setDate(1);
      dateTime.setHours(0);
      dateTime.setMinutes(0);
      dateTime.setSeconds(0);
      dateTime.setMilliseconds(0);
    }
  }

  return dateTime;
};

export default Axis;
