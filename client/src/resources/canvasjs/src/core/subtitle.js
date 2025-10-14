import Title from "./title";
import CanvasJSObject from "./canvasjs";
import { extend } from "../helpers/utils";

function Subtitle(chart, options) {
  Subtitle.base.constructor.call(this, "Subtitle", options, chart.theme);

  this.chart = chart;
  this.canvas = chart.canvas;
  this.ctx = this.chart.ctx;

  if (typeof this._options.fontSize === "undefined") {
    this.fontSize = this.chart.getAutoFontSize(this.fontSize);
  }

  ((this.width = null), (this.height = null));
  this.bounds = {
    x1: null,
    y1: null,
    x2: null,
    y2: null,
  };
}

extend(Subtitle, CanvasJSObject);

Subtitle.prototype.render = Title.prototype.render;

export default Subtitle;
