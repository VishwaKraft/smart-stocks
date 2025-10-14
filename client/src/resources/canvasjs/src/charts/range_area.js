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
    var closingPath = [];

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

    var isFirstDataPointInPlotArea = true;
    var i = 0,
      x,
      y1,
      y2;
    var dataPointX;

    var startPoint = null;

    if (dataPoints.length > 0) {
      var color = dataSeries._colorSet[i % dataSeries._colorSet.length];
      ctx.fillStyle = color;
      ctx.strokeStyle = color;
      ctx.lineWidth = dataSeries.lineThickness;

      if (ctx.setLineDash) {
        ctx.setLineDash(
          getLineDashArray(dataSeries.lineDashType, dataSeries.lineThickness),
        );
      }

      var prevDataNull = true;
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
          closeArea();

          prevDataNull = true;
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

        if (isFirstDataPointInPlotArea || prevDataNull) {
          ctx.beginPath();
          ctx.moveTo(x, y1);
          startPoint = {
            x: x,
            y: y1,
          };
          closingPath = [];
          closingPath.push({ x: x, y: y2 });

          if (isCanvasSupported) {
            ghostCtx.beginPath();
            ghostCtx.moveTo(x, y1);
          }

          isFirstDataPointInPlotArea = false;
          prevDataNull = false;
        } else {
          ctx.lineTo(x, y1);
          closingPath.push({ x: x, y: y2 });

          if (isCanvasSupported) ghostCtx.lineTo(x, y1);

          if (i % 250 == 0) {
            closeArea();
          }
        }

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

        if (dataPoints[i].markerSize !== 0) {
          if (dataPoints[i].markerSize > 0 || dataSeries.markerSize > 0) {
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

            markerProps = dataSeries.getMarkerProperties(i, x, y1, ctx);
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
          }
        }

        if (
          dataPoints[i].indexLabel ||
          dataSeries.indexLabel ||
          dataPoints[i].indexLabelFormatter ||
          dataSeries.indexLabelFormatter
        ) {
          this._indexLabels.push({
            chartType: "rangeArea",
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
            chartType: "rangeArea",
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

      closeArea();

      RenderHelper.drawMarkers(markers);
    }
  }

  ctx.restore();
  if (isCanvasSupported) this._eventManager.ghostCtx.restore();

  function closeArea() {
    if (!startPoint) return;

    var point = null;

    if (dataSeries.lineThickness > 0) ctx.stroke();

    for (var i = closingPath.length - 1; i >= 0; i--) {
      point = closingPath[i];
      ctx.lineTo(point.x, point.y);
      ghostCtx.lineTo(point.x, point.y);
    }

    ctx.closePath();

    ctx.globalAlpha = dataSeries.fillOpacity;
    ctx.fill();
    ctx.globalAlpha = 1;

    ghostCtx.fill();

    if (dataSeries.lineThickness > 0) {
      ctx.beginPath();
      ctx.moveTo(point.x, point.y);
      for (var i = 0; i < closingPath.length; i++) {
        point = closingPath[i];
        ctx.lineTo(point.x, point.y);
      }

      ctx.stroke();
    }

    ctx.beginPath();
    ctx.moveTo(x, y1);
    ghostCtx.beginPath();
    ghostCtx.moveTo(x, y1);

    startPoint = {
      x: x,
      y: y1,
    };
    closingPath = [];
    closingPath.push({ x: x, y: y2 });
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
