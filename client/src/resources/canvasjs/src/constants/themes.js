import { isCanvasSupported } from "../helpers/utils";

export const colorSets = {
  colorSet1: [
    "#369EAD",
    "#C24642",
    "#7F6084",
    "#86B402",
    "#A2D1CF",
    "#C8B631",
    "#6DBCEB",
    "#52514E",
    "#4F81BC",
    "#A064A1",
    "#F79647",
  ],
  colorSet2: [
    "#4F81BC",
    "#C0504E",
    "#9BBB58",
    "#23BFAA",
    "#8064A1",
    "#4AACC5",
    "#F79647",
    "#33558B",
  ],
  colorSet3: [
    "#8CA1BC",
    "#36845C",
    "#017E82",
    "#8CB9D0",
    "#708C98",
    "#94838D",
    "#F08891",
    "#0366A7",
    "#008276",
    "#EE7757",
    "#E5BA3A",
    "#F2990B",
    "#03557B",
    "#782970",
  ],
};

export const themes = {
  theme1: {
    Chart: {
      colorSet: "colorSet1",
    },
    Title: {
      fontFamily: isCanvasSupported
        ? "Calibri, Optima, Candara, Verdana, Geneva, sans-serif"
        : "calibri",
      fontSize: 33,
      fontColor: "#3A3A3A",
      fontWeight: "bold",
      verticalAlign: "top",
      margin: 5,
    },
    Subtitle: {
      fontFamily: isCanvasSupported
        ? "Calibri, Optima, Candara, Verdana, Geneva, sans-serif"
        : "calibri",
      fontSize: 16,
      fontColor: "#3A3A3A",
      fontWeight: "bold",
      verticalAlign: "top",
      margin: 5,
    },
    Axis: {
      titleFontSize: 26,
      titleFontColor: "#666666",
      titleFontFamily: isCanvasSupported
        ? "Calibri, Optima, Candara, Verdana, Geneva, sans-serif"
        : "calibri",

      labelFontFamily: isCanvasSupported
        ? "Calibri, Optima, Candara, Verdana, Geneva, sans-serif"
        : "calibri",
      labelFontSize: 18,
      labelFontColor: "grey",
      tickColor: "#BBBBBB",
      tickThickness: 2,
      gridThickness: 2,
      gridColor: "#BBBBBB",
      lineThickness: 2,
      lineColor: "#BBBBBB",
    },
    Legend: {
      verticalAlign: "bottom",
      horizontalAlign: "center",
      fontFamily: isCanvasSupported
        ? "monospace, sans-serif,arial black"
        : "calibri",
    },
    DataSeries: {
      indexLabelFontColor: "grey",
      indexLabelFontFamily: isCanvasSupported
        ? "Calibri, Optima, Candara, Verdana, Geneva, sans-serif"
        : "calibri",
      indexLabelFontSize: 18,
      indexLabelLineThickness: 1,
    },
  },

  theme2: {
    Chart: {
      colorSet: "colorSet2",
    },
    Title: {
      fontFamily: "impact, charcoal, arial black, sans-serif",
      fontSize: 32,
      fontColor: "#333333",
      verticalAlign: "top",
      margin: 5,
    },
    Subtitle: {
      fontFamily: "impact, charcoal, arial black, sans-serif",
      fontSize: 14,
      fontColor: "#333333",
      verticalAlign: "top",
      margin: 5,
    },
    Axis: {
      titleFontSize: 22,
      titleFontColor: "rgb(98,98,98)",
      titleFontFamily: isCanvasSupported
        ? "monospace, sans-serif,arial black"
        : "arial",
      titleFontWeight: "bold",

      labelFontFamily: isCanvasSupported
        ? "monospace, Courier New, Courier"
        : "arial",
      labelFontSize: 16,
      labelFontColor: "grey",
      labelFontWeight: "bold",
      tickColor: "grey",
      tickThickness: 2,
      gridThickness: 2,
      gridColor: "grey",
      lineColor: "grey",
      lineThickness: 0,
    },
    Legend: {
      verticalAlign: "bottom",
      horizontalAlign: "center",
      fontFamily: isCanvasSupported
        ? "monospace, sans-serif,arial black"
        : "arial",
    },
    DataSeries: {
      indexLabelFontColor: "grey",
      indexLabelFontFamily: isCanvasSupported
        ? "Courier New, Courier, monospace"
        : "arial",
      indexLabelFontWeight: "bold",
      indexLabelFontSize: 18,
      indexLabelLineThickness: 1,
    },
  },

  theme3: {
    Chart: {
      colorSet: "colorSet1",
    },
    Title: {
      fontFamily: isCanvasSupported
        ? "Candara, Optima, Trebuchet MS, Helvetica Neue, Helvetica, Trebuchet MS, serif"
        : "calibri",
      fontSize: 32,
      fontColor: "#3A3A3A",
      fontWeight: "bold",
      verticalAlign: "top",
      margin: 5,
    },
    Subtitle: {
      fontFamily: isCanvasSupported
        ? "Candara, Optima, Trebuchet MS, Helvetica Neue, Helvetica, Trebuchet MS, serif"
        : "calibri",
      fontSize: 16,
      fontColor: "#3A3A3A",
      fontWeight: "bold",
      verticalAlign: "top",
      margin: 5,
    },
    Axis: {
      titleFontSize: 22,
      titleFontColor: "rgb(98,98,98)",
      titleFontFamily: isCanvasSupported
        ? "Verdana, Geneva, Calibri, sans-serif"
        : "calibri",

      labelFontFamily: isCanvasSupported
        ? "Calibri, Optima, Candara, Verdana, Geneva, sans-serif"
        : "calibri",
      labelFontSize: 18,
      labelFontColor: "grey",
      tickColor: "grey",
      tickThickness: 2,
      gridThickness: 2,
      gridColor: "grey",
      lineThickness: 2,
      lineColor: "grey",
    },
    Legend: {
      verticalAlign: "bottom",
      horizontalAlign: "center",
      fontFamily: isCanvasSupported
        ? "monospace, sans-serif,arial black"
        : "calibri",
    },
    DataSeries: {
      bevelEnabled: true,
      indexLabelFontColor: "grey",
      indexLabelFontFamily: isCanvasSupported
        ? "Candara, Optima, Calibri, Verdana, Geneva, sans-serif"
        : "calibri",
      indexLabelFontSize: 18,
      indexLabelLineColor: "lightgrey",
      indexLabelLineThickness: 2,
    },
  },
};
