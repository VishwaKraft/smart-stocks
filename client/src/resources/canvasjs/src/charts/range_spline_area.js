import { isCanvasSupported } from "../helpers/utils";

export default function (plotUnit) {
  var ctx = plotUnit.targetCanvasCtx || this.plotArea.ctx;
  var totalDataSeries = plotUnit.dataSeriesIndexes.length;

  if (totalDataSeries <= 0) return;

  var ghostCtx = this._eventManager.ghostCtx;

  var markers = [];

  var plotArea = this.plotArea;
  ctx.save();

  if (isCanvasSupported) ghostCtx.save();

  ctx.beginPath();
  ctx.rect(plotArea.x1, plotArea.y1, plotArea.width, plotArea.height);
  ctx.clip();

  if (isCanvasSupported) {
    ghostCtx.beginPath();
    ghostCtx.rect(plotArea.x1, plotArea.y1, plotArea.width, plotArea.height);
    ghostCtx.clip();
  }

  for (var j = 0; j < plotUnit.dataSeriesIndexes.length; j++) {
    var dataSeriesIndex = plotUnit.dataSeriesIndexes[j];

    var dataSeries = this.data[dataSeriesIndex];

    var dataPoints = dataSeries.dataPoints;

    var seriesId = dataSeries.id;
    this._eventManager.objectMap[seriesId] = {
      objectType: "dataSeries",
      dataSeriesIndex: dataSeriesIndex,
    };

    var hexColor = intToHexColorString(seriesId);
    ghostCtx.fillStyle = hexColor;

    markers = [];

    var i = 0,
      x,
      y1,
      y2;
    var dataPointX;

    var pixelsY1 = [];
    var pixelsY2 = [];

    if (dataPoints.length > 0) {
      color = dataSeries._colorSet[i % dataSeries._colorSet.length];
      ctx.fillStyle = color;
      ctx.strokeStyle = color;
      ctx.lineWidth = dataSeries.lineThickness;

      if (ctx.setLineDash) {
        ctx.setLineDash(
          getLineDashArray(dataSeries.lineDashType, dataSeries.lineThickness),
        );
      }

      for (; i < dataPoints.length; i++) {
        dataPointX = dataPoints[i].x.getTime
          ? dataPoints[i].x.getTime()
          : dataPoints[i].x;

        if (
          dataPointX < plotUnit.axisX.dataInfo.viewPortMin ||
          dataPointX > plotUnit.axisX.dataInfo.viewPortMax
        ) {
          continue;
        }

        if (
          dataPoints[i].y === null ||
          !dataPoints[i].y.length ||
          typeof dataPoints[i].y[0] !== "number" ||
          typeof dataPoints[i].y[1] !== "number"
        ) {
          if (i > 0) {
            renderBezierArea();
            pixelsY1 = [];
            pixelsY2 = [];
          }
          continue;
        }

        x =
          (plotUnit.axisX.conversionParameters.reference +
            plotUnit.axisX.conversionParameters.pixelPerUnit *
              (dataPointX - plotUnit.axisX.conversionParameters.minimum) +
            0.5) <<
          0;
        y1 =
          (plotUnit.axisY.conversionParameters.reference +
            plotUnit.axisY.conversionParameters.pixelPerUnit *
              (dataPoints[i].y[0] -
                plotUnit.axisY.conversionParameters.minimum) +
            0.5) <<
          0;
        y2 =
          (plotUnit.axisY.conversionParameters.reference +
            plotUnit.axisY.conversionParameters.pixelPerUnit *
              (dataPoints[i].y[1] -
                plotUnit.axisY.conversionParameters.minimum) +
            0.5) <<
          0;

        var id = dataSeries.dataPointIds[i];
        this._eventManager.objectMap[id] = {
          id: id,
          objectType: "dataPoint",
          dataSeriesIndex: dataSeriesIndex,
          dataPointIndex: i,
          x1: x,
          y1: y1,
          y2: y2,
        };

        pixelsY1[pixelsY1.length] = {
          x: x,
          y: y1,
        };
        pixelsY2[pixelsY2.length] = {
          x: x,
          y: y2,
        };

        if (dataPoints[i].markerSize !== 0) {
          if (dataPoints[i].markerSize > 0 || dataSeries.markerSize > 0) {
            var markerProps = dataSeries.getMarkerProperties(i, x, y1, ctx);
            markers.push(markerProps);

            var markerColor = intToHexColorString(id);

            if (isCanvasSupported) {
              markers.push({
                x: x,
                y: y1,
                ctx: ghostCtx,
                type: markerProps.type,
                size: markerProps.size,
                color: markerColor,
                borderColor: markerColor,
                borderThickness: markerProps.borderThickness,
              });
            }

            var markerProps = dataSeries.getMarkerProperties(i, x, y2, ctx);
            markers.push(markerProps);

            var markerColor = intToHexColorString(id);

            if (isCanvasSupported) {
              markers.push({
                x: x,
                y: y2,
                ctx: ghostCtx,
                type: markerProps.type,
                size: markerProps.size,
                color: markerColor,
                borderColor: markerColor,
                borderThickness: markerProps.borderThickness,
              });
            }
          }
        }

        if (
          dataPoints[i].indexLabel ||
          dataSeries.indexLabel ||
          dataPoints[i].indexLabelFormatter ||
          dataSeries.indexLabelFormatter
        ) {
          this._indexLabels.push({
            chartType: "splineArea",
            dataPoint: dataPoints[i],
            dataSeries: dataSeries,
            indexKeyword: 0,
            point: {
              x: x,
              y: y1,
            },
            direction: dataPoints[i].y[0] <= dataPoints[i].y[1] ? -1 : 1,
            color: color,
          });

          this._indexLabels.push({
            chartType: "splineArea",
            dataPoint: dataPoints[i],
            dataSeries: dataSeries,
            indexKeyword: 1,
            point: {
              x: x,
              y: y2,
            },
            direction: dataPoints[i].y[0] <= dataPoints[i].y[1] ? 1 : -1,
            color: color,
          });
        }
      }

      renderBezierArea();

      RenderHelper.drawMarkers(markers);
    }
  }

  ctx.restore();

  if (isCanvasSupported) this._eventManager.ghostCtx.restore();

  function renderBezierArea() {
    var bp = getBezierPoints(pixelsY1, 2);

    if (bp.length > 0) {
      ctx.beginPath();
      ctx.moveTo(bp[0].x, bp[0].y);

      if (isCanvasSupported) {
        ghostCtx.beginPath();
        ghostCtx.moveTo(bp[0].x, bp[0].y);
      }

      for (var i = 0; i < bp.length - 3; i += 3) {
        ctx.bezierCurveTo(
          bp[i + 1].x,
          bp[i + 1].y,
          bp[i + 2].x,
          bp[i + 2].y,
          bp[i + 3].x,
          bp[i + 3].y,
        );

        if (isCanvasSupported)
          ghostCtx.bezierCurveTo(
            bp[i + 1].x,
            bp[i + 1].y,
            bp[i + 2].x,
            bp[i + 2].y,
            bp[i + 3].x,
            bp[i + 3].y,
          );
      }

      if (dataSeries.lineThickness > 0) ctx.stroke();

      bp = getBezierPoints(pixelsY2, 2);

      ctx.lineTo(
        pixelsY2[pixelsY2.length - 1].x,
        pixelsY2[pixelsY2.length - 1].y,
      );

      for (var i = bp.length - 1; i > 2; i -= 3) {
        ctx.bezierCurveTo(
          bp[i - 1].x,
          bp[i - 1].y,
          bp[i - 2].x,
          bp[i - 2].y,
          bp[i - 3].x,
          bp[i - 3].y,
        );

        if (isCanvasSupported)
          ghostCtx.bezierCurveTo(
            bp[i - 1].x,
            bp[i - 1].y,
            bp[i - 2].x,
            bp[i - 2].y,
            bp[i - 3].x,
            bp[i - 3].y,
          );
      }

      ctx.closePath();

      ctx.globalAlpha = dataSeries.fillOpacity;
      ctx.fill();
      ctx.globalAlpha = 1;

      if (dataSeries.lineThickness > 0) {
        ctx.beginPath();
        ctx.moveTo(
          pixelsY2[pixelsY2.length - 1].x,
          pixelsY2[pixelsY2.length - 1].y,
        );

        for (var i = bp.length - 1; i > 2; i -= 3) {
          ctx.bezierCurveTo(
            bp[i - 1].x,
            bp[i - 1].y,
            bp[i - 2].x,
            bp[i - 2].y,
            bp[i - 3].x,
            bp[i - 3].y,
          );

          if (isCanvasSupported)
            ghostCtx.bezierCurveTo(
              bp[i - 1].x,
              bp[i - 1].y,
              bp[i - 2].x,
              bp[i - 2].y,
              bp[i - 3].x,
              bp[i - 3].y,
            );
        }
        ctx.stroke();
      }

      ctx.beginPath();

      if (isCanvasSupported) {
        ghostCtx.closePath();
        ghostCtx.fill();
      }
    }
  }

  var animationInfo = {
    source: ctx,
    dest: this.plotArea.ctx,
    animationCallback: AnimationHelper.xClipAnimation,
    easingFunction: AnimationHelper.easing.linear,
    animationBase: 0,
  };
  return animationInfo;
}

var drawSegment = function (
  ctx,
  center,
  radius,
  color,
  type,
  theta1,
  theta2,
  fillOpacity,
  percentInnerRadius,
) {
  if (typeof fillOpacity === "undefined") fillOpacity = 1;

  if (!isCanvasSupported) {
    var theta2Mod = Number((theta2 % (2 * Math.PI)).toFixed(8));
    var theta1Mod = Number((theta1 % (2 * Math.PI)).toFixed(8));
    if (theta1Mod === theta2Mod) theta2 -= 0.0001;
  }

  ctx.save();
  ctx.globalAlpha = fillOpacity;

  if (type === "pie") {
    ctx.beginPath();
    ctx.moveTo(center.x, center.y);
    ctx.arc(center.x, center.y, radius, theta1, theta2, false);
    ctx.fillStyle = color;
    ctx.strokeStyle = "white";
    ctx.lineWidth = 2;
    ctx.closePath();
    ctx.fill();
  } else if (type === "doughnut") {
    ctx.beginPath();
    ctx.arc(center.x, center.y, radius, theta1, theta2, false);
    ctx.arc(
      center.x,
      center.y,
      percentInnerRadius * radius,
      theta2,
      theta1,
      true,
    );
    ctx.closePath();
    ctx.fillStyle = color;
    ctx.strokeStyle = "white";
    ctx.lineWidth = 2;
    ctx.fill();
  }

  ctx.globalAlpha = 1;

  ctx.restore();
};
