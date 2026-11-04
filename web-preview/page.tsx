"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import featuredJson from "../../public/content/featured.json";
import {
  BookOpen,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Clock,
  FileQuestion,
  Flag,
  Home as HomeIcon,
  Layers,
  Loader2,
  Mic,
  NotebookPen,
  Palette,
  Pause,
  Play,
  RotateCcw,
  Settings as SettingsIcon,
  Trash2,
  TrendingUp,
  Timer,
  X,
  Droplet,
} from "lucide-react";

/* ================================ helpers ================================ */
const FA = "۰۱۲۳۴۵۶۷۸۹";
const faNum = (v: number | string): string =>
  String(v).replace(/\d/g, (d) => FA[Number(d)]);

const faDose = (cc: number): string => {
  const v = Math.round(cc * 100) / 100;
  const s = Number.isInteger(v) ? String(v) : v.toFixed(2);
  return faNum(s).replace(".", "٫");
};

const jalaliLong = (ms: number): string => {
  const parts = new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  }).formatToParts(new Date(ms));
  const get = (t: string) => parts.find((p) => p.type === t)?.value ?? "";
  return `${get("weekday")}، ${get("day")} ${get("month")} ${get("year")}`;
};

/* ---- تقویم شمسی (جلالی) ---- */
const JALALI_MONTHS = ["فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"];

const jalaliPartsOf = (ms: number): { jy: number; jm: number; jd: number } => {
  const parts = new Intl.DateTimeFormat("en-US-u-ca-persian", { year: "numeric", month: "numeric", day: "numeric" }).formatToParts(new Date(ms));
  const get = (t: string) => Number((parts.find((p) => p.type === t)?.value ?? "0").replace(/[^\d]/g, ""));
  return { jy: get("year"), jm: get("month"), jd: get("day") };
};

/** تبدیل تاریخ جلالی به مُهرِ نیمه‌شب میلادی */
const jalaliToMs = (jy: number, jm: number, jd: number): number => {
  let jyy = jy + 1595;
  let days =
    -355668 + 365 * jyy + Math.floor(jyy / 33) * 8 + Math.floor(((jyy % 33) + 3) / 4) + jd +
    (jm < 7 ? (jm - 1) * 31 : (jm - 7) * 30 + 186);
  let gy = 400 * Math.floor(days / 146097);
  days %= 146097;
  if (days > 36524) {
    gy += 100 * Math.floor(--days / 36524);
    days %= 36524;
    if (days >= 365) days++;
  }
  gy += 4 * Math.floor(days / 1461);
  days %= 1461;
  if (days > 365) {
    gy += Math.floor((days - 1) / 365);
    days = (days - 1) % 365;
  }
  let gd = days + 1;
  const leap = (y: number) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
  const md = [31, leap(gy) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  let gm = 0;
  while (gm < 12 && gd > md[gm]) {
    gd -= md[gm];
    gm++;
  }
  return normalizeDay(new Date(gy, gm, gd).getTime());
};

const shuffle = <T,>(arr: T[]): T[] => {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
};

const normalizeDay = (ms: number): number => {
  const d = new Date(ms);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};

/* ================================ themes (مطابق Theme.kt اپ) ================================ */
type ThemeColors = {
  primary: string;
  onPrimary: string;
  container: string;
  onContainer: string;
  bg: string;
  onBg: string;
  surface: string;
  surfaceVariant: string;
  onSurfaceVariant: string;
  outline: string;
  outlineVariant: string;
  topBar: string;
  topBarContent: string;
  secondary: string;
  secondaryContainer: string;
  tertiary: string;
  tertiaryContainer: string;
  onTertiaryContainer: string;
};

type ThemeModeDef = {
  id: string;
  labelFa: string;
  swatch: string;
  dark?: boolean;
  c: ThemeColors;
};

/* رنگ اختصاصی هر تب — مطابق الگوی «پلنر من»: هر تب هویت رنگی خودش را دارد
   و کارت‌ها/دکمه‌های همان تب از همان خانوادهٔ رنگی استفاده می‌کنند */
const NAV_COLORS = {
  home: "#F97316",
  plan: "#14B8A6",
  journal: "#8B5CF6",
  exam: "#EC4899",
  lib: "#F59E0B",
  settings: "#8B5CF6", // رنگ بخش «تقویم» برنامه «پلنر من» (#8B5CF6) — طبق درخواست کاربر
};

/* رنگ صورتی کارت «روز پاک من» و چیپ «پیشرفت سفر» در تب خانه — عمداً ثابتِ مستقل از NAV_COLORS تعریف شده
   تا رنگ اصلی برنامه (کارت خانه) هرگز با تغییر رنگ تب آزمون عوض نشود؛
   ریشهٔ «تغییر مکرر ظاهر برنامه» در پیش‌نمایش، وابستگی قبلی گرادیان کارت خانه به رنگ آزمون بود */
const HOME_HERO_PINK = "#EC4899";

/* رنگ نقطهٔ شماره‌دار هر مادهٔ مصرفی — الگو گرفته از چیپ‌های شماره‌دار «مواد مصرفی» (عکس ۱):
   ۱ نارنجی، ۲ فیروزه‌ای، ۳ سرخابی، ۴ رز، ۵ بنفش، ۶ کهربایی، ۷ سبز، ۸ خاکستری */
const SUBSTANCE_DOTS: Record<SubstanceId, string> = {
  OPIUM_SMOKED: "#F97316",
  OPIUM_ORAL: "#14B8A6",
  HEROIN: "#EC4899",
  ALCOHOL: "#E11D48",
  STIMULANT: "#8B5CF6",
  METHADONE: "#F59E0B",
  TRAMADOL: "#10B981",
  SHIREH_ORAL: "#64748B",
  SOKHTE: "#B45309",
  SLEEPING: "#475569",
  OTHER: "#78716C",
};
type NavColorKey = keyof typeof NAV_COLORS;

const GREEN_OK = "#2E7D32";
const RED_NO = "#C62828";
const AMBER = "#F9A825";

const THEMES: ThemeModeDef[] = [
  {
    id: "NARENJI",
    labelFa: "نارنجی گرم",
    swatch: "#F97316",
    c: {
      primary: "#EA580C",
      onPrimary: "#FFFFFF",
      container: "#FFE8D4",
      onContainer: "#7C2D12",
      bg: "#FFF9EC",
      onBg: "#292524",
      surface: "#FFFFFF",
      surfaceVariant: "#F7EFDD",
      onSurfaceVariant: "#78716C",
      outline: "#B8A88E",
      outlineVariant: "#EADFC8",
      topBar: "#FFF9EC",
      topBarContent: "#292524",
      secondary: "#0D9488",
      secondaryContainer: "#DCF3F0",
      tertiary: "#F9A825",
      tertiaryContainer: "#FFE9B8",
      onTertiaryContainer: "#4A3A00",
    },
  },
  {
    id: "BANAFSH",
    labelFa: "بنفش",
    swatch: "#6A0DAD",
    c: {
      primary: "#6A0DAD",
      onPrimary: "#FFFFFF",
      container: "#F0E3FF",
      onContainer: "#2A0A55",
      bg: "#F9F6FD",
      onBg: "#1B1723",
      surface: "#FFFFFF",
      surfaceVariant: "#EDE6F6",
      onSurfaceVariant: "#38323F",
      outline: "#655D75",
      outlineVariant: "#CCC5D6",
      topBar: "#6A0DAD",
      topBarContent: "#FFFFFF",
      secondary: "#7B1FA2",
      secondaryContainer: "#F3E5F5",
      tertiary: "#F9A825",
      tertiaryContainer: "#FFE9B8",
      onTertiaryContainer: "#4A3A00",
    },
  },
  {
    id: "ARGHAVANI",
    labelFa: "ارغوانی",
    swatch: "#A21CAF",
    c: {
      primary: "#A21CAF",
      onPrimary: "#FFFFFF",
      container: "#FAE3F7",
      onContainer: "#4A0A44",
      bg: "#FDF7FB",
      onBg: "#221A21",
      surface: "#FFFFFF",
      surfaceVariant: "#F4E4F1",
      onSurfaceVariant: "#3A2F38",
      outline: "#7F657A",
      outlineVariant: "#DCC9D6",
      topBar: "#A21CAF",
      topBarContent: "#FFFFFF",
      secondary: "#B1549F",
      secondaryContainer: "#F8E4F3",
      tertiary: "#F9A825",
      tertiaryContainer: "#FFE9B8",
      onTertiaryContainer: "#4A3A00",
    },
  },
  {
    id: "ORCHID",
    labelFa: "ارکیده",
    swatch: "#9C3794",
    c: {
      primary: "#9C3794",
      onPrimary: "#FFFFFF",
      container: "#F7E1F3",
      onContainer: "#3B1038",
      bg: "#FCF7FB",
      onBg: "#211B21",
      surface: "#FFFFFF",
      surfaceVariant: "#F2E3EF",
      onSurfaceVariant: "#372F36",
      outline: "#7A6176",
      outlineVariant: "#D8C6D4",
      topBar: "#9C3794",
      topBarContent: "#FFFFFF",
      secondary: "#AF5CA6",
      secondaryContainer: "#F5E3F2",
      tertiary: "#F9A825",
      tertiaryContainer: "#FFE9B8",
      onTertiaryContainer: "#4A3A00",
    },
  },
  {
    id: "KHAKESTARI",
    labelFa: "خاکستری آرام",
    swatch: "#52525B",
    c: {
      primary: "#52525B",
      onPrimary: "#FFFFFF",
      container: "#E8E8EB",
      onContainer: "#232327",
      bg: "#F7F7F8",
      onBg: "#1C1C1F",
      surface: "#FFFFFF",
      surfaceVariant: "#EEEEF0",
      onSurfaceVariant: "#3A3A3F",
      outline: "#71717A",
      outlineVariant: "#D9D9DE",
      topBar: "#52525B",
      topBarContent: "#FFFFFF",
      secondary: "#6B7280",
      secondaryContainer: "#ECECEE",
      tertiary: "#F9A825",
      tertiaryContainer: "#FFE9B8",
      onTertiaryContainer: "#4A3A00",
    },
  },
  {
    id: "AUTO",
    labelFa: "خودکار",
    swatch: "#F97316",
    dark: true,
    c: {
      primary: "#D3B8FF",
      onPrimary: "#2E0A5C",
      container: "#452080",
      onContainer: "#EADDFF",
      bg: "#121016",
      onBg: "#E6E1EC",
      surface: "#1A1721",
      surfaceVariant: "#2B2634",
      onSurfaceVariant: "#C9C2D4",
      outline: "#948CA3",
      outlineVariant: "#3B3546",
      topBar: "#1A1721",
      topBarContent: "#E6E1EC",
      secondary: "#C7AEEF",
      secondaryContainer: "#3A2350",
      tertiary: "#FFC94D",
      tertiaryContainer: "#4A3A00",
      onTertiaryContainer: "#FFE9B8",
    },
  },
];

/* ================================ پروتکل DST (مطابق Protocol.kt) ================================ */
type StepKind = "START" | "BUILDUP" | "STABILIZE" | "REDUCE" | "FINAL";
const KIND_LABEL: Record<StepKind, string> = {
  START: "شروع",
  BUILDUP: "افزایش",
  STABILIZE: "تثبیت",
  REDUCE: "کاهش",
  FINAL: "پله آخر",
};

type PlanStep = {
  title: string;
  kind: StepKind;
  dosesPerDay: number;
  perDoseCc: number;
  days: number;
  note: string;
};
type SubstanceId =
  | "OPIUM_SMOKED"
  | "OPIUM_ORAL"
  | "HEROIN"
  | "ALCOHOL"
  | "STIMULANT"
  | "METHADONE"
  | "TRAMADOL"
  | "SHIREH_ORAL"
  | "SOKHTE"
  | "SLEEPING"
  | "OTHER";
type SubstanceDef = {
  id: SubstanceId;
  label: string;
  program: string;
  consumptionLabel: string | null;
  consumptionHint: string;
  options?: number[];
  optionUnit?: (v: number) => string;
  /** توضیح پویای مقدار انتخاب‌شده — با هر کلیک روی اعداد، متن زیر اعداد عوض می‌شود */
  consumptionDetail?: (v: number) => string;
};
type UserSettings = {
  startDate: number | null;
  substance: SubstanceId;
  consumption: number;
  doseModel: number;
};
type PersonalPlan = {
  steps: PlanStep[];
  startDailyCc: number;
  capDailyCc: number;
  notes: string[];
};

const STEP_DAYS = 21;
const STABILIZE_DAYS = 22;

const OT_LADDER: PlanStep[] = [
  { title: "پله اول", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 5.5, days: STEP_DAYS, note: "نامه هفتگی — نامه اول ۸ روز (۱۳۲cc)" },
  { title: "پله دوم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 4.5, days: STEP_DAYS, note: "نامه هفتگی (۹۴٫۵cc)" },
  { title: "پله سوم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 3.5, days: STEP_DAYS, note: "نامه هفتگی (۷۳٫۵cc)" },
  { title: "پله چهارم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 3.0, days: STEP_DAYS, note: "نامه ۲ هفته‌ای با صلاحدید راهنما (۶۳cc)" },
  { title: "پله پنجم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 2.5, days: STEP_DAYS, note: "نامه ۲ هفته‌ای با صلاحدید راهنما (۵۲٫۵cc)" },
  { title: "پله ششم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 2.0, days: STEP_DAYS, note: "نامه ۲ هفته‌ای با صلاحدید راهنما (۴۲cc)" },
  { title: "پله هفتم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 1.5, days: STEP_DAYS, note: "نامه ۳ هفته‌ای با صلاحدید راهنما (۳۱٫۵cc)" },
  { title: "پله هشتم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 1.2, days: STEP_DAYS, note: "نامه ۳ هفته‌ای با صلاحدید راهنما (۲۵٫۲cc)" },
  { title: "پله نهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 1.0, days: STEP_DAYS, note: "نامه ۳ هفته‌ای با صلاحدید راهنما (۲۱cc)" },
  { title: "پله دهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.8, days: STEP_DAYS, note: "نامه ۳ هفته‌ای با صلاحدید راهنما (۱۶٫۸cc)" },
  { title: "پله یازدهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.6, days: STEP_DAYS, note: "نامه ۳ هفته‌ای با صلاحدید راهنما (۱۲٫۶cc)" },
  { title: "پله دوازدهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.4, days: STEP_DAYS, note: "نامه ۶ هفته‌ای با صلاحدید راهنما (۸٫۴cc)" },
  { title: "پله سیزدهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.3, days: STEP_DAYS, note: "نامه ۶ هفته‌ای با صلاحدید راهنما (۶٫۳cc)" },
  { title: "پله چهاردهم (BID)", kind: "REDUCE", dosesPerDay: 2, perDoseCc: 0.3, days: STEP_DAYS, note: "حذف یک وعده — نامه ۶ هفته‌ای (۴٫۲cc)" },
  { title: "پله پانزدهم — آخر (DAILY)", kind: "FINAL", dosesPerDay: 1, perDoseCc: 0.3, days: 7, note: "یک وعده ۰٫۳cc؛ ۲ تا ۲۱ روز و سپس دستور قطع" },
];

const STIMULANT_LADDER: PlanStep[] = [
  { title: "هفته اول", kind: "START", dosesPerDay: 1, perDoseCc: 0.3, days: 7, note: "یک وعده ۰٫۳cc (Daily)" },
  { title: "هفته دوم", kind: "START", dosesPerDay: 2, perDoseCc: 0.3, days: 7, note: "دو وعده ۰٫۳cc (BID)" },
  { title: "پله اول", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 0.3, days: STEP_DAYS, note: "روزانه ۰٫۹cc" },
  { title: "پله دوم", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 0.4, days: STEP_DAYS, note: "روزانه ۱٫۲cc" },
  { title: "پله سوم", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 0.6, days: STEP_DAYS, note: "روزانه ۱٫۸cc" },
  { title: "پله چهارم", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 0.8, days: STEP_DAYS, note: "روزانه ۲٫۴cc" },
  { title: "پله پنجم", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 1.0, days: STEP_DAYS, note: "روزانه ۳cc" },
  { title: "پله ششم", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 1.2, days: STEP_DAYS, note: "روزانه ۳٫۶cc" },
  { title: "پله هفتم", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 1.5, days: STEP_DAYS, note: "روزانه ۴٫۵cc" },
  { title: "پله هشتم (تثبیت)", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: 1.5, days: STEP_DAYS, note: "روزانه ۴٫۵cc — توقف روی سقف" },
  { title: "پله نهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 1.2, days: STEP_DAYS, note: "روزانه ۳٫۶cc" },
  { title: "پله دهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 1.0, days: STEP_DAYS, note: "روزانه ۳cc" },
  { title: "پله یازدهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.8, days: STEP_DAYS, note: "روزانه ۲٫۴cc" },
  { title: "پله دوازدهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.6, days: STEP_DAYS, note: "روزانه ۱٫۸cc" },
  { title: "پله سیزدهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.4, days: STEP_DAYS, note: "روزانه ۱٫۲cc" },
  { title: "پله چهاردهم", kind: "REDUCE", dosesPerDay: 3, perDoseCc: 0.3, days: STEP_DAYS, note: "روزانه ۰٫۹cc" },
  { title: "پله پانزدهم", kind: "REDUCE", dosesPerDay: 2, perDoseCc: 0.3, days: STEP_DAYS, note: "BID — روزانه ۰٫۶cc" },
  { title: "هفته آخر", kind: "FINAL", dosesPerDay: 1, perDoseCc: 0.3, days: 7, note: "Daily — ۲ تا ۲۱ روز" },
];

const OT_LADDER_NOTES = [
  "صلاحدید در دوره صدور نامه فقط برای رهجوی مرتب، منظم و بدون غیبت است؛ در غیر این صورت دارو هفتگی تجویز می‌شود.",
  "تلورانس هر نامه (هر چند هفته) حداکثر ۳ سی‌سی است.",
  "نامه هفته اول رهجو برای ۸ روز محاسبه می‌شود.",
  "پله آخر (روزانه ۰٫۳ سی‌سی OT) می‌تواند ۲ تا ۲۱ روز ادامه داشته باشد.",
];

const SUBSTANCES: SubstanceDef[] = [
  {
    id: "OPIUM_SMOKED",
    label: "تریاک کشیدنی",
    program: "برنامه ۶–۳",
    consumptionLabel: null,
    consumptionHint:
      "تریاک و شیره کشیدنی به هر مقدار و هر شیوه (وافور، سیخ‌وسنگ، قرقری) و همچنین کرک: مصرف بلافاصله قطع و همزمان شربت OT شروع می‌شود؛ نیازی به ثبت مقدار نیست.",
  },
  {
    id: "OPIUM_ORAL",
    label: "تریاک خوراکی",
    program: "معادل‌سازی مستقیم یا برنامه ۹–۱۶٫۵",
    consumptionLabel: "میزان مصرف روزانه (گرم)",
    consumptionHint:
      "تا ۱٫۶۵ گرم: هر ۱۰۰ میلی‌گرم تریاک معادل ۱ سی‌سی OT. بیشتر از آن: برنامه ۹–۱۶٫۵ (خاخام) اجرا می‌شود.",
    options: [0.5, 1.0, 1.5, 2.0],
    consumptionDetail: (v) =>
      v <= 0.75
        ? "یعنی روزانه ۰٫۵ گرم (۵۰۰ میلی‌گرم) تریاک خوراکی. معادل آن ۵ سی‌سی OT است؛ این مقدار به سه وعدهٔ مساوی (هر وعده ≈ ۱٫۷cc) تقسیم و یک پلهٔ ۲۱ روزه ثابت می‌ماند و سپس تیپرینگ با ضریب ۰٫۸ آغاز می‌شود."
        : v <= 1.25
          ? "یعنی روزانه ۱ گرم (۱۰۰۰ میلی‌گرم) تریاک خوراکی. معادل آن ۱۰ سی‌سی OT است؛ این مقدار به سه وعدهٔ مساوی (هر وعده ≈ ۳٫۳cc) تقسیم می‌شود؛ یک پلهٔ ۲۱ روزه ثابت و سپس تیپرینگ با ضریب ۰٫۸."
          : v <= 1.75
            ? "یعنی روزانه ۱٫۵ گرم (۱۵۰۰ میلی‌گرم) تریاک خوراکی. معادل آن ۱۵ سی‌سی OT است؛ به سه وعدهٔ مساوی (هر وعده ۵cc) تقسیم می‌شود؛ یک پلهٔ ۲۱ روزه ثابت و سپس تیپرینگ با ضریب ۰٫۸."
            : "یعنی روزانه ۲ گرم تریاک خوراکی. معادل آن ۲۰ سی‌سی OT است؛ چون از سقف ۱۶٫۵cc بیشتر است، معادل‌سازی مستقیم اجرا نمی‌شود و «برنامهٔ ۹–۱۶٫۵» اجرا می‌شود: سه روز اول روزانه ۹cc (هر وعده ۳cc)، سپس سقف ۱۶٫۵cc (هر وعده ۵٫۵cc)، ۲۲ روز تثبیت و بعد کاهش طبق جدول OT.",
  },
  {
    id: "HEROIN",
    label: "هروئین",
    program: "برنامه هروئین",
    consumptionLabel: null,
    consumptionHint:
      "هر شکل و هر مقدار: از روز نخست با روزانه ۶ سی‌سی OT شروع می‌شود؛ سقف ۱۰٫۵ سی‌سی.",
  },
  {
    id: "ALCOHOL",
    label: "الکل",
    program: "تیپر ۶۱ روزه الکل + متد شیشه (سقف ۶cc)",
    consumptionLabel: null,
    consumptionHint:
      "ابتدا الکل TDS و طی ۶۱ روز تیپر و قطع می‌شود؛ سپس OT مثل متد شیشه با سقف ۶ سی‌سی.",
  },
  {
    id: "STIMULANT",
    label: "شیشه و محرک‌ها",
    program: "جدول ۱۷ ردیفی محرک‌ها",
    consumptionLabel: null,
    consumptionHint:
      "شیشه، حشیش، گل، ماری‌جوانا، کوکائین، قرص اکستازی و قرص B2: شروع با ۰٫۳ سی‌سی در روز و صعود پله‌به‌پله تا تثبیت ۴٫۵ سی‌سی.",
  },
  {
    id: "METHADONE",
    label: "متادون",
    program: "جایگزینی هفتگی وعده‌به‌وعده با OT",
    consumptionLabel: "مصرف روزانه شربت متادون (سی‌سی)",
    consumptionHint:
      "قرص ۵ میلی‌گرمی = ۱ سی‌سی شربت متادون = ۱ سی‌سی شربت OT؛ قرص ۱۰mg = ۲cc، قرص ۲۰mg = ۴cc و قرص ۴۰mg = ۸cc.",
    options: [5, 10, 16.5],
    consumptionDetail: (v) =>
      v <= 7.5
        ? "یعنی روزانه ۵ سی‌سی شربت متادون مصرف می‌کنی (معادل ۲۵ میلی‌گرم متادون). در برنامه، دقیقاً ۵ سی‌سی شربت OT جای همان ۵ سی‌سی متادون می‌نشیند: یک هفته هر سه وعده متادون می‌خوری، سپس هفته‌ای یک وعده (اول ظهر، بعد صبح، بعد شب) با OT جایگزین می‌شود، ۲۲ روز فقط OT تثبیت می‌شود و بعد کاهش طبق جدول آغاز می‌شود."
        : v <= 13
          ? "یعنی روزانه ۱۰ سی‌سی شربت متادون مصرف می‌کنی (معادل ۵۰ میلی‌گرم متادون). در برنامه، دقیقاً ۱۰ سی‌سی شربت OT جای همان ۱۰ سی‌سی متادون می‌نشیند: یک هفته هر سه وعده متادون، سپس هفته‌ای یک وعده (اول ظهر، بعد صبح، بعد شب) با OT جایگزین می‌شود، ۲۲ روز تثبیت و بعد کاهش طبق جدول."
          : "یعنی روزانه ۱۶٫۵ سی‌سی شربت متادون مصرف می‌کنی (معادل ۸۲٫۵ میلی‌گرم متادون) — این سقف مجاز پروتکل DST است. در برنامه دقیقاً ۱۶٫۵ سی‌سی OT جای همان مقدار می‌نشیند: جایگزینی هفته‌به‌هفتهٔ وعده‌ها (ظهر، صبح، شب)، ۲۲ روز تثبیت روی سقف و سپس کاهش از پلهٔ اول جدول OT (هر وعده ۵٫۵cc).",
  },
  {
    id: "TRAMADOL",
    label: "ترامادول",
    program: "مثل متادون — قرص ۱۰۰mg = ۰٫۵cc OT",
    consumptionLabel: "تعداد قرص ۱۰۰ میلی‌گرمی در روز",
    consumptionHint:
      "هر قرص ۱۰۰ میلی‌گرمی = ۰٫۵ سی‌سی OT؛ قرص ۲۰۰ میلی‌گرمی = ۱ سی‌سی. اگر OT محاسبه‌شده کمتر از ۶cc شد، تا سقف ۶cc بالا می‌رود.",
    options: [2, 4, 6, 10],
    optionUnit: (v) => `${faNum(v)} قرص`,
    consumptionDetail: (v) =>
      v <= 3
        ? "یعنی روزانه ۲ قرص ۱۰۰ میلی‌گرمی ترامادول (یا ۱ قرص ۲۰۰ میلی‌گرمی). معادل آن ۱cc OT است؛ چون محاسبه‌شده کمتر از ۶cc است، طبق قاعدهٔ برنامه تا سقف ۶cc بالا برده می‌شود و درمان با روزانه ۶cc OT (هر وعده ۲cc) شروع می‌شود."
        : v <= 5
          ? "یعنی روزانه ۴ قرص ۱۰۰ میلی‌گرمی ترامادول. معادل آن ۲cc OT است؛ چون کمتر از ۶cc است، تا سقف ۶cc بالا می‌رود و درمان با روزانه ۶cc OT (هر وعده ۲cc) شروع می‌شود."
          : v <= 8
            ? "یعنی روزانه ۶ قرص ۱۰۰ میلی‌گرمی ترامادول. معادل آن ۳cc OT است؛ باز کمتر از ۶cc است، پس برنامه با روزانه ۶cc OT (هر وعده ۲cc) اجرا می‌شود."
            : "یعنی روزانه ۱۰ قرص ۱۰۰ میلی‌گرمی ترامادول. معادل آن ۵cc OT است؛ باز هم کمتر از ۶cc است، پس درمان با روزانه ۶cc OT (هر وعده ۲cc) شروع می‌شود.",
  },
  {
    id: "SHIREH_ORAL",
    label: "شیره خوراکی",
    program: "محاسبه مثل تریاک خوراکی + جدول جایگزینی",
    consumptionLabel: "میزان مصرف روزانه (گرم)",
    consumptionHint:
      "محاسبه مثل تریاک خوراکی؛ سپس هفته‌ای یک وعده (ظهر، صبح، شب) با OT جایگزین می‌شود.",
    options: [0.5, 1.0, 1.5, 2.0],
    consumptionDetail: (v) =>
      v <= 0.75
        ? "یعنی روزانه ۰٫۵ گرم (۵۰۰ میلی‌گرم) شیرهٔ خوراکی. معادل آن ۵ سی‌سی OT است؛ محاسبه مثل تریاک خوراکی است و سپس طبق جدول چهار هفته‌ای اصلاحیه، هفته‌ای یک وعده (اول ظهر، بعد صبح، بعد شب) با OT جایگزین می‌شود تا هر سه وعده OT شود."
        : v <= 1.25
          ? "یعنی روزانه ۱ گرم شیرهٔ خوراکی. معادل آن ۱۰ سی‌سی OT است؛ محاسبه مثل تریاک خوراکی و سپس جایگزینیِ هفته‌ای یک وعده (اول ظهر، بعد صبح، بعد شب) با OT طبق جدول چهار هفته‌ای اصلاحیه."
          : v <= 1.75
            ? "یعنی روزانه ۱٫۵ گرم شیرهٔ خوراکی. معادل آن ۱۵ سی‌سی OT است؛ محاسبه مثل تریاک خوراکی و سپس جایگزینیِ هفته‌ای یک وعده (ظهر، صبح، شب) با OT."
            : "یعنی روزانه ۲ گرم شیرهٔ خوراکی. معادل آن ۲۰ سی‌سی OT است؛ چون از سقف ۱۶٫۵cc بیشتر است، ابتدا طی حداکثر دو هفته مصرف شیره کاهش و به سه وعده تقسیم می‌شود تا به معادل ۱۶٫۵cc برسد؛ سپس جایگزینی هفته‌ای یک وعده (ظهر، صبح، شب) با OT انجام می‌شود.",
  },
  {
    id: "SOKHTE",
    label: "سوخته تریاک",
    program: "برنامه ۶–۳ (مثل تریاک و شیره کشیدنی)",
    consumptionLabel: null,
    consumptionHint:
      "سوخته (باقی‌ماندهٔ سوختهٔ تریاک/شیره) به هر مقدار و هر شیوه: مصرف بلافاصله قطع و همزمان شربت OT شروع می‌شود؛ طبق بخش ۱ اصلاحیه، همین برنامهٔ ۶–۳ اجرا می‌شود.",
  },
  {
    id: "SLEEPING",
    label: "قرص",
    program: "جدول مستقل ندارد — فقط با هماهنگی راهنما",
    consumptionLabel: null,
    consumptionHint:
      "طبق «راهنما و راهنمایی»: قرص‌های خواب و آرام‌بخش هم روی سیستم ایکس اثر می‌گذارند (آنتی‌اکس)؛ در متد DST برای آن‌ها جدول مستقلی ثبت نشده و درمان فقط با هماهنگی راهنما انجام می‌شود.",
  },
  {
    id: "OTHER",
    label: "سایر",
    program: "برنامهٔ پیش‌فرض ندارد — تنظیم با هماهنگی راهنما",
    consumptionLabel: null,
    consumptionHint:
      "برای هر مادهٔ دیگری، نوع ماده و برنامهٔ درمان باید فقط با هماهنگی راهنمای خودت ثبت شود؛ این گزینه جدول محاسباتی پیش‌فرض ندارد.",
  },
];

const defaultConsumption = (id: SubstanceId): number => {
  if (id === "OPIUM_ORAL" || id === "SHIREH_ORAL") return 1.0;
  if (id === "METHADONE") return 10.0;
  if (id === "TRAMADOL") return 4.0;
  return 0.0;
};

const r1 = (v: number) => Math.round(v * 10) / 10;
const r2 = (v: number) => Math.round(v * 100) / 100;
const dailyOf = (s: PlanStep) => r2(s.perDoseCc * s.dosesPerDay);

function ladderFrom(dailyCc: number): PlanStep[] {
  const idx = OT_LADDER.findIndex((s) => dailyOf(s) <= dailyCc + 0.001);
  return idx < 0 ? OT_LADDER.slice(-1) : OT_LADDER.slice(idx);
}

function buildPlan(settings: UserSettings): PersonalPlan {
  const { substance, consumption } = settings;
  if (substance === "SLEEPING" || substance === "OTHER") {
    const isSleep = substance === "SLEEPING";
    return {
      steps: [
        {
          title: "برنامه‌ریزی فقط با هماهنگی راهنما",
          kind: "START",
          dosesPerDay: 3,
          perDoseCc: 0,
          days: 1,
          note: isSleep
            ? "برای قرص‌های خواب جدول محاسباتی مستقلی در متد DST ثبت نشده است؛ نوع دارو، مقدار و زمان‌بندی را فقط راهنمای تو تعیین می‌کند."
            : "برای این ماده برنامهٔ محاسباتی پیش‌فرضی وجود ندارد؛ نوع ماده و برنامهٔ درمان را فقط با هماهنگی راهنمای خودت ثبت کن.",
        },
      ],
      startDailyCc: 0,
      capDailyCc: 0,
      notes: [
        isSleep
          ? "«راهنما و راهنمایی»: قرص‌های خواب و آرام‌بخش هم روی سیستم ایکس اثر می‌گذارند (آنتی‌اکس)؛ درمان اعتیاد با جایگزینی این داروها نتیجه‌بخش نبوده است."
          : "متد DST برای تریاک، شیره، سوخته، هروئین، متادون، ترامادول، الکل و محرک‌ها جدول دارد؛ بقیهٔ مواد فقط با دستور راهنما.",
      ],
    };
  }
  if (substance === "OPIUM_SMOKED" || substance === "SOKHTE") {
    const steps: PlanStep[] = [
      { title: "شروع — روزهای نخست", kind: "START", dosesPerDay: 3, perDoseCc: 1.0, days: 3, note: "هر وعده ۱cc (روزانه ۳cc)؛ پس از گذشت ۳ روز، مقدار دو برابر می‌شود" },
      { title: "پله صعودی", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 2.0, days: STEP_DAYS, note: "هر وعده ۲cc (روزانه ۶cc)" },
      { title: "افزایش تا سقف", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 2.5, days: STEP_DAYS, note: "هر وعده ۲٫۵cc (روزانه ۷٫۵cc) — تقسیم ۶ بر ضریب ۰٫۸" },
      { title: "تثبیت روی سقف", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: 2.5, days: STABILIZE_DAYS, note: "سقف برنامه ۶–۳: هر وعده ۲٫۵cc (روزانه ۷٫۵cc) — «۲۲ روز یا دو پله» توقف" },
    ];
    steps.push(...ladderFrom(7.5));
    return {
      steps,
      startDailyCc: 3.0,
      capDailyCc: 7.5,
      notes: [
        "برنامه ۶–۳ اصلاحیه ۱۳۹۸: شروع با هر وعده ۱cc، دو برابر کردن پس از ۳ روز، صعود تا سقف ۷٫۵cc و پس از ۲۲ روز توقف، کاهش طبق جدول.",
        "مصرف کشیدنی بلافاصله قطع و همزمان OT شروع می‌شود؛ برای تریاک، شیره کشیدنی، سوخته و کرک به هر مقدار و هر شیوه همین برنامه اجرا می‌شود.",
      ],
    };
  }
  if (substance === "OPIUM_ORAL") {
    const g = consumption > 0 ? consumption : 1.0;
    if (g <= 1.65) {
      const daily = r1(g * 10.0);
      const steps: PlanStep[] = [
        { title: "پله اول — معادل‌سازی و TDS", kind: "START", dosesPerDay: 3, perDoseCc: r2(daily / 3), days: STEP_DAYS, note: "مقدار معادل به سه وعده مساوی تقسیم و یک پله ۲۱ روزه ثابت می‌ماند" },
      ];
      if (daily < 16.5) {
        const up = Math.min(daily / 0.8, 16.5);
        steps.push({ title: "پله افزایشی (اختیاری)", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: r2(up / 3), days: STEP_DAYS, note: "در صورت کمتر بودن از سقف، به تشخیص راهنما می‌توان یک پله افزایش داد (تقسیم بر ۰٫۸)" });
      }
      steps.push({ title: "تکرار پله (اختیاری)", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: r2(steps[steps.length - 1].perDoseCc), days: STEP_DAYS, note: "پس از تکرار، عملیات تیپرینگ با ضریب ۰٫۸ آغاز می‌شود" });
      steps.push(...ladderFrom(daily));
      return {
        steps,
        startDailyCc: daily,
        capDailyCc: 16.5,
        notes: [
          "مصرف ≤ ۱٫۶۵ گرم: هر ۱۰۰ میلی‌گرم تریاک = ۱cc شربت OT؛ مقدار معادل TDS و پله اول برنامه است.",
          "پس از یک پله ۲۱ روزه می‌توان تکرار پله و تیپرینگ با ضریب ۰٫۸ را آغاز کرد؛ رُند کردن دقیق مقدار با نظر راهنما انجام می‌شود.",
        ],
      };
    }
    const steps: PlanStep[] = [
      { title: "شروع برنامه ۹–۱۶٫۵", kind: "START", dosesPerDay: 3, perDoseCc: 3.0, days: 3, note: "سه روز اول: هر وعده ۳cc (روزانه ۹cc)" },
      { title: "رسیدن به سقف", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 5.5, days: STEP_DAYS, note: "پس از ۳ روز: هر وعده ۵٫۵cc (روزانه ۱۶٫۵cc)" },
      { title: "تثبیت روی سقف", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: 5.5, days: STABILIZE_DAYS, note: "یک پله تکرار روی سقف ۱۶٫۵cc — «۲۲ روز یا دو پله»" },
    ];
    steps.push(...ladderFrom(16.5));
    return {
      steps,
      startDailyCc: 9.0,
      capDailyCc: 16.5,
      notes: [
        "مصرف > ۱٫۶۵ گرم: برنامه ۹–۱۶٫۵ (خاخام) اجرا می‌شود.",
        "طبق ویرایش ۹۹ (ص۴۹): سه روز روزانه ۹cc، سپس سقف ۱۶٫۵cc، یک پله تکرار و بعد تیپرینگ با ضریب ۰٫۸ طبق جدول OT.",
      ],
    };
  }
  if (substance === "HEROIN") {
    const steps: PlanStep[] = [
      { title: "شروع", kind: "START", dosesPerDay: 3, perDoseCc: 2.0, days: STEP_DAYS, note: "از روز نخست هر وعده ۲cc (روزانه ۶cc)؛ ۲۱ روز ثابت" },
      { title: "افزایش تا سقف", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: 3.5, days: STEP_DAYS, note: "هر وعده ۳٫۵cc (روزانه ۱۰٫۵cc)" },
      { title: "تثبیت روی سقف", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: 3.5, days: STABILIZE_DAYS, note: "«۲۲ روز یا دو پله» توقف و بعد کاهش طبق جدول تا صفر" },
    ];
    steps.push(...ladderFrom(10.5));
    return {
      steps,
      startDailyCc: 6.0,
      capDailyCc: 10.5,
      notes: [
        "اصلاحیه ۱۳۹۸: شروع از روز نخست با روزانه ۶cc؛ سقف برنامه ۳ × ۳٫۵ = ۱۰٫۵cc است (عدد «۱۱/۵» در اسکن، خطای تایپی است و با سقف ویرایش ۹۹ ص۵۰ و ص۶۵ سازگار نیست).",
        "هروئین به‌صورت تدریجی قابل تیپر نیست؛ بلافاصله قطع و با OT جایگزین و سپس طبق جدول کاهش داده می‌شود.",
      ],
    };
  }
  if (substance === "STIMULANT") {
    return {
      steps: STIMULANT_LADDER,
      startDailyCc: 0.3,
      capDailyCc: 4.5,
      notes: [
        "شیشه، حشیش، گل، ماری‌جوانا، کوکائین، قرص اکستازی و قرص B2 همه با همین جدول درمان می‌شوند.",
        "هفته اول یک وعده ۰٫۳cc، هفته دوم دو وعده (BID) و از پله اول سه وعده (TDS).",
        "پله تثبیت، پله هشتم با روزانه ۴٫۵cc است؛ هفته آخر ۲ تا ۲۱ روز ادامه دارد.",
        "طبق متد DST: در مصرف همزمان چند ماده، مصرف غالب ملاک تجویز OT است؛ الکل پروتکل جداگانه دارد و ۶۱ روز تیپر می‌شود (اصلاحیه پروتکل — بند درمان الکل).",
      ],
    };
  }
  if (substance === "METHADONE") {
    if (consumption <= 16.5) {
      const daily = consumption > 0 ? r1(consumption) : 10.0;
      const per = r2(daily / 3);
      const steps: PlanStep[] = [
        { title: "TDS شربت متادون", kind: "START", dosesPerDay: 3, perDoseCc: per, days: 7, note: "مصرف به سه وعده مساوی تقسیم می‌شود؛ یک هفته جهت سازگاری (می‌توان برای رُند شدن طبق جدول گرد کرد)" },
        { title: "جایگزینی وعده ظهر", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح متادون · ظهر OT · شب متادون" },
        { title: "جایگزینی وعده صبح", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح OT · ظهر OT · شب متادون" },
        { title: "جایگزینی وعده شب و تثبیت", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: per, days: STABILIZE_DAYS, note: "هر سه وعده OT — «۲۲ روز یا دو پله» توقف جهت سازگاری" },
      ];
      steps.push(...ladderFrom(daily));
      return {
        steps,
        startDailyCc: daily,
        capDailyCc: 16.5,
        notes: [
          "حالت اول اصلاحیه (≤ ۱۶٫۵cc): دقیقاً به همان میزان متادون، OT تجویز و وعده‌به‌وعده (ظهر، صبح، شب) جایگزین می‌شود.",
          "یک قرص متادون ۵ میلی‌گرمی = ۱cc شربت متادون = ۱cc شربت OT؛ قرص ۱۰mg = ۲cc، قرص ۲۰mg = ۴cc و قرص ۴۰mg = ۸cc.",
        ],
      };
    }
    const per = r2(16.5 / 3);
    const steps: PlanStep[] = [
      { title: "کاهش همزمان و TDS متادون", kind: "START", dosesPerDay: 3, perDoseCc: per, days: 14, note: "کاهش تدریجی و تقسیم به سه وعده تا نزدیک ۱۶٫۵cc طی حداکثر ۲ هفته" },
      { title: "جایگزینی وعده ظهر", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح متادون · ظهر OT · شب متادون" },
      { title: "جایگزینی وعده صبح", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح OT · ظهر OT · شب متادون" },
      { title: "جایگزینی وعده شب و تثبیت", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: per, days: STABILIZE_DAYS, note: "هر سه وعده OT — «۲۲ روز یا دو پله» توقف" },
    ];
    steps.push(...ladderFrom(16.5));
    return {
      steps,
      startDailyCc: 16.5,
      capDailyCc: 16.5,
      notes: ["حالت دوم اصلاحیه (> ۱۶٫۵cc): ابتدا طی حداکثر دو هفته به نزدیک ۱۶٫۵cc می‌رسیم، سپس مراحل جایگزینی OT."],
    };
  }
  if (substance === "TRAMADOL") {
    const pills = consumption > 0 ? consumption : 4.0;
    const computed = r2(pills * 0.5); // قرص ۱۰۰ میلی‌گرمی = ۰٫۵cc — طبق پانویس اصلاحیهٔ بخش ۶ و جدول «راهنما و راهنمایی»
    const daily = Math.min(Math.max(computed, 6.0), 16.5);
    const raised = computed < 6.0;
    const per = r2(daily / 3);
    const steps: PlanStep[] = [
      { title: "TDS قرص ترامادول", kind: "START", dosesPerDay: 3, perDoseCc: per, days: 7, note: "معادل OT به سه وعده مساوی تقسیم می‌شود؛ یک هفته جهت سازگاری" },
      { title: "جایگزینی وعده ظهر", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح ترامادول · ظهر OT · شب ترامادول" },
      { title: "جایگزینی وعده صبح", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح OT · ظهر OT · شب ترامادول" },
      { title: "جایگزینی وعده شب و تثبیت", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: per, days: STABILIZE_DAYS, note: "هر سه وعده OT — «۲۲ روز یا دو پله» توقف" },
    ];
    steps.push(...ladderFrom(daily));
    return {
      steps,
      startDailyCc: daily,
      capDailyCc: 16.5,
      notes: [
        "هر قرص ترامادول ۱۰۰ میلی‌گرمی = ۰٫۵cc شربت OT و قرص ۲۰۰ میلی‌گرمی = ۱cc (پانویس اصلاحیهٔ بخش ۶ و جدول «راهنما و راهنمایی»).",
        "برنامه دقیقاً مانند متادون اجرا می‌شود (جایگزینی هفتگی وعده‌به‌وعده + ۲۲ روز تثبیت)." +
          (raised ? " چون مقدار محاسبه‌شده کمتر از ۶cc بود، تا سقف ۶cc بالا برده می‌شود." : ""),
      ],
    };
  }
  if (substance === "SHIREH_ORAL") {
    const g = consumption > 0 ? consumption : 1.0;
    if (g > 1.65) {
      const per = r2(16.5 / 3);
      const steps: PlanStep[] = [
        { title: "کاهش همزمان و TDS شیره", kind: "START", dosesPerDay: 3, perDoseCc: per, days: 14, note: "کاهش تدریجی و تقسیم به سه وعده تا معادل ۱۶٫۵cc OT طی حداکثر ۲ هفته" },
        { title: "جایگزینی وعده ظهر", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح شیره · ظهر OT · شب شیره" },
        { title: "جایگزینی وعده صبح", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح OT · ظهر OT · شب شیره" },
        { title: "جایگزینی وعده شب و تثبیت", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: per, days: STABILIZE_DAYS, note: "هر سه وعده OT — «۲۲ روز یا دو پله» توقف" },
      ];
      steps.push(...ladderFrom(16.5));
      return {
        steps,
        startDailyCc: 16.5,
        capDailyCc: 16.5,
        notes: ["معادلسازی شیره خوراکی دقیقاً مثل تریاک خوراکی است؛ چون معادل از سقف بیشتر است، ابتدا طی دو هفته به نزدیک ۱۶٫۵cc کاهش و سپس جایگزینی هفتگی انجام می‌شود."],
      };
    }
    const daily = Math.min(r1(g * 10.0), 16.5);
    const per = r2(daily / 3);
    const steps: PlanStep[] = [
      { title: "TDS شیره خوراکی", kind: "START", dosesPerDay: 3, perDoseCc: per, days: 7, note: "مقدار شیره به سه وعده مساوی تقسیم می‌شود؛ یک هفته" },
      { title: "جایگزینی وعده ظهر", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح شیره · ظهر OT · شب شیره" },
      { title: "جایگزینی وعده صبح", kind: "BUILDUP", dosesPerDay: 3, perDoseCc: per, days: 7, note: "صبح OT · ظهر OT · شب شیره" },
      { title: "جایگزینی وعده شب و تثبیت", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: per, days: STABILIZE_DAYS, note: "هر سه وعده OT — «۲۲ روز یا دو پله» توقف" },
    ];
    steps.push(...ladderFrom(daily));
    return {
      steps,
      startDailyCc: daily,
      capDailyCc: 16.5,
      notes: ["محاسبه دقیقاً مثل تریاک خوراکی است (هر ۱۰۰ میلی‌گرم = ۱cc OT)؛ جایگزینی، هفته‌ای یک وعده طبق جدول‌های چهار هفته‌ای اصلاحیه انجام می‌شود."],
    };
  }
  // ALCOHOL
  const steps: PlanStep[] = [
    { title: "تثبیت و TDS الکل", kind: "START", dosesPerDay: 3, perDoseCc: 0.0, days: 7, note: "مقدار الکل روزانه محاسبه و به سه وعده مساوی تقسیم می‌شود" },
    { title: "تیپر الکل", kind: "START", dosesPerDay: 3, perDoseCc: 0.0, days: 61, note: "طی ۶۱ روز الکل پله‌پله کم و سپس قطع می‌شود (اصلاحیه بخش ۸؛ ≈ «تیپر دو ماهه» ویرایش ۹۹)" },
  ];
  steps.push(...STIMULANT_LADDER.slice(2, 9));
  steps.push({ title: "پله سقف الکل", kind: "STABILIZE", dosesPerDay: 3, perDoseCc: 2.0, days: STABILIZE_DAYS, note: "سقف OT در درمان الکل: هر وعده ۲cc (روزانه ۶cc) — «۲۲ روز یا دو پله» توقف" });
  steps.push(...ladderFrom(6.0));
  return {
    steps,
    startDailyCc: 0.3,
    capDailyCc: 6.0,
    notes: [
      "فقط برای فردی که کاملاً به الکل معتاد شده و مرتب و منظم و حاضر در جلسات است، OT «همانند متد شیشه» جایگزین می‌شود.",
      "سقف OT در درمان الکل ۶ سی‌سی روزانه است؛ بین مصرف گاه‌به‌گاه و مصرف روزانه منظم تفاوت زیادی وجود دارد.",
      "قطع ناگهانی الکل در وابستگی شدید می‌تواند تشنج بیاورد؛ تیپر ۶۱ روزه را خودسرانه کوتاه نکن.",
    ],
  };
}

const currentStepIndex = (plan: PersonalPlan, dayIndex: number): number | null => {
  let acc = 0;
  for (let i = 0; i < plan.steps.length; i++) {
    acc += plan.steps[i].days;
    if (dayIndex < acc) return i;
  }
  return null;
};

const stepStartDay = (plan: PersonalPlan, index: number): number =>
  plan.steps.slice(0, index).reduce((s, st) => s + st.days, 0);

const daysSinceStart = (settings: UserSettings, now: number): number => {
  if (settings.startDate == null) return 0;
  return Math.floor((normalizeDay(now) - normalizeDay(settings.startDate)) / 86400000);
};

const doseTimes = (model: number): string[] => {
  if (model === 1) return ["صبح — ۰۵:۰۰", "ظهر — ۱۳:۰۰", "شب — ۲۱:۰۰"];
  if (model === 3) return ["صبح — ۰۷:۰۰", "ظهر — ۱۵:۰۰", "شب — ۲۳:۰۰"];
  return ["صبح — ۰۶:۰۰", "ظهر — ۱۴:۰۰", "شب — ۲۲:۰۰"];
};

/* ================================ بانک سؤال (asset اپ) ================================ */
type BankMcq = { id: string; q: string; opts: Record<string, string>; ans: string; cat: string; ref: string; src: string };
type BankQa = { id: string; q: string; a: string; cat: string; ref: string; src: string };
type Bank = { mcq: BankMcq[]; qa: BankQa[] };

const LETTER_ORDER = ["الف", "ب", "ج", "د"] as const;

type QuizItem = { id?: string; question: string; options: string[]; correctIndex: number; explanation: string; cat: string };

/* ذخیره‌گاه سؤال‌های غلط — مبنای کارت «مرور اشتباه‌ها» */
const WRONG_KEY = "hamrah_wrong_v1";
const loadWrong = (): QuizItem[] => {
  try {
    const v = JSON.parse(localStorage.getItem(WRONG_KEY) ?? "[]") as unknown;
    return Array.isArray(v)
      ? (v as QuizItem[]).filter((x) => x && typeof x.question === "string" && Array.isArray(x.options) && typeof x.correctIndex === "number")
      : [];
  } catch {
    return [];
  }
};
const saveWrong = (list: QuizItem[]) => {
  try {
    localStorage.setItem(WRONG_KEY, JSON.stringify(list.slice(-120)));
  } catch {
    /* بی‌صدا */
  }
};

/**
 * چرخش هوشمند سؤال‌ها — آخرین سؤال‌های دیده‌شده (تا ۴۰۰) ذخیره می‌شود تا
 * هر بار ورود به آزمون، تا جای ممکن سؤال تازه بیاید و سؤال‌های سشن قبل تکرار نشوند.
 */
const RECENT_MAX = 400;
const loadRecent = (mode: string): string[] => {
  try {
    const v = JSON.parse(localStorage.getItem(`hamrah_recent_${mode}`) ?? "[]") as unknown;
    return Array.isArray(v) ? (v as string[]).filter((x) => typeof x === "string") : [];
  } catch {
    return [];
  }
};
const saveRecent = (mode: string, ids: string[]) => {
  try {
    const merged = [...new Set([...loadRecent(mode), ...ids])].slice(-RECENT_MAX);
    localStorage.setItem(`hamrah_recent_${mode}`, JSON.stringify(merged));
  } catch {
    /* بی‌صدا */
  }
};
/* چینش سشن جدید: اول سؤال‌های تازه، بعد سؤال‌های دیده‌شده — هر گروه خودش شافل می‌شود */
const rotateFresh = <T,>(pool: [string, T][], recent: string[]): T[] => {
  const seen = new Set(recent);
  const fresh = shuffle(pool.filter(([id]) => !seen.has(id)).map(([, x]) => x));
  const old = shuffle(pool.filter(([id]) => seen.has(id)).map(([, x]) => x));
  return [...fresh, ...old];
};

const CORE_DST: QuizItem[] = [
  { question: "در متد DST، حرف S به چه معناست؟", options: ["شروع درمان", "پله ۲۱ روزه", "سقف مصرف", "ثبت روزانه"], correctIndex: 1, explanation: "S = Step یعنی پله ۲۱ روزه؛ D ضریب ۰٫۸ و T حداقل ۱۰ ماه درمان است.", cat: "متد DST" },
  { question: "ضریب کاهش مصرف در هر پله متد DST چقدر است؟", options: ["۰٫۵", "۰٫۸", "۱٫۲", "۲"], correctIndex: 1, explanation: "در هر پله، مقدار مصرف در ضریب ۰٫۸ (D = Dezhakam) ضرب می‌شود.", cat: "متد DST" },
  { question: "فاصله وعده‌های مصرف در متد DST چند ساعت است؟", options: ["۶ ساعت", "۸ ساعت", "۱۲ ساعت", "۲۴ ساعت"], correctIndex: 1, explanation: "سه وعده با فاصله ۸ ساعت (مثلاً ۶ صبح، ۲ بعدازظهر و ۱۰ شب — مدل ۲).", cat: "متد DST" },
  { question: "سقف OT در برنامه ۶–۳ چند سی‌سی در روز است؟", options: ["۶ سی‌سی", "۷٫۵ سی‌سی", "۹ سی‌سی", "۱۶٫۵ سی‌سی"], correctIndex: 1, explanation: "سقف برنامه ۶–۳: هر وعده ۲٫۵cc و روزانه ۷٫۵cc؛ سپس ۲۲ روز توقف روی سقف.", cat: "متد DST" },
  { question: "توقف تثبیت روی سقف مصرف طبق اصلاحیه چند روز است؟", options: ["۷ روز", "۱۴ روز", "۲۲ روز", "۳۰ روز"], correctIndex: 2, explanation: "عبارت اصلاحیه: «۲۲ روز یا دو پله» توقف روی سقف.", cat: "متد DST" },
  { question: "هر ۱ سی‌سی شربت اپیوم تینکچر (OT) معادل چند میلی‌گرم تریاک خوراکی است؟", options: ["۵۰ میلی‌گرم", "۱۰۰ میلی‌گرم", "۲۰۰ میلی‌گرم", "۵۰۰ میلی‌گرم"], correctIndex: 1, explanation: "هر ۱cc OT = ۱۰۰ میلی‌گرم تریاک خوراکی؛ بنابراین ۱ گرم تریاک = ۱۰cc.", cat: "متد DST" },
  { question: "در برنامه ۶–۳ با مصرف کشیدنی چه باید کرد؟", options: ["کاهش تدریجی مصرف کشیدنی", "قطع همان شب هفته اول", "قطع بلافاصله و شروع همزمان OT", "فقط ثبت روزانه کافی است"], correctIndex: 2, explanation: "مصرف کشیدنی بلافاصله قطع و همزمان شربت OT شروع می‌شود.", cat: "متد DST" },
  { question: "پله آخر برنامه (روزانه ۰٫۳cc) چند روز ادامه دارد؟", options: ["۳ روز", "۷ روز ثابت", "۲ تا ۲۱ روز", "۲۱ روز ثابت"], correctIndex: 2, explanation: "پله آخر طبق توضیح جدول، ۲ تا ۲۱ روز ادامه دارد و سپس با نظر راهنما قطع می‌شود.", cat: "متد DST" },
  { question: "سقف OT در درمان الکل چند سی‌سی در روز است؟", options: ["۴٫۵ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"], correctIndex: 1, explanation: "سقف OT در درمان الکل ۶ سی‌سی روزانه است (هر وعده ۲cc).", cat: "متد DST" },
  { question: "در درمان هروئین، شروع OT از روز نخست با روزانه چند سی‌سی است؟", options: ["۳ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"], correctIndex: 1, explanation: "اصلاحیه ۱۳۹۸: از روز نخست روزانه ۶cc (هر وعده ۲cc)؛ سقف برنامه ۱۰٫۵cc است.", cat: "متد DST" },
];

const CORE_CAT = "متد DST";

const mcqToQuiz = (m: BankMcq): QuizItem => {
  const keys: string[] = [];
  const texts: string[] = [];
  for (const k of LETTER_ORDER) {
    const v = m.opts?.[k] ?? "";
    if (v.trim()) {
      keys.push(k);
      texts.push(v.trim());
    }
  }
  const ansIndex = keys.indexOf(m.ans);
  const safe = ansIndex >= 0 ? ansIndex : 0;
  const explanation =
    "پاسخ درست: " + (texts[safe] ?? "") + (m.ref ? " · " + m.ref : "") + " · منبع: " + m.cat;
  return { id: m.id, question: m.q, options: texts, correctIndex: safe, explanation, cat: m.cat };
};

/* ================================ کتابخانه ================================ */
type LibItem = {
  id: string;
  title: string;
  sub: string;
  color: string;
  text: string;
};

const LIB_ITEMS: LibItem[] = [
  {
    id: "lib-01",
    title: "چهارده مقاله",
    sub: "مقالات • مهندس حسین دژاکام •",
    color: NAV_COLORS.home,
    text: "کتاب «چهارده مقاله» مجموعهٔ چهارده مقالهٔ آموزشی مهندس حسین دژاکام، بنیان‌گذار کنگره ۶۰ است. مقاله‌ها با توصیف حال امروزِ ما آغاز می‌شوند: ما که امروز در این جمع نشسته‌ایم، روزی با مواد مخدر زندگی می‌کردیم؛ کانال‌های عصبی بدنمان تحت تأثیر مواد قرار گرفته بود و تفکرمان درست عمل نمی‌کرد. با ورود به کنگره ۶۰ و پذیرش بیماری، راه درمان را آغاز کردیم. در ادامهٔ کتاب، سه ضلع درمان — جسم، روان و جهان‌بینی — یک‌به‌یک با راه‌حل‌های عملی شرح داده می‌شود. پیام کتاب روشن است: اعتیاد بیماری است و این بیماری، درمان دارد؛ ما می‌توانیم.",
  },
  {
    id: "lib-02",
    title: "کنگره ۶۰ چیست؟",
    sub: "معرفی • سابقه و اهداف کنگرهٔ ۶۰ •",
    color: NAV_COLORS.plan,
    text: "کنگره ۶۰ تشکیلات تحقیقاتی مستقلی است که برای رهایی مصرف‌کنندگان مواد مخدر از دام اعتیاد شکل گرفته است؛ برای کسانی که همواره خواستار ترک بودند، اما راه‌حل منطقی برای درمان خود پیدا نکرده بودند. اساس شکل‌گیری کنگره بر این اصل است: فردی که خودش رها شده، به فردی که هنوز در حال مصرف است کمک می‌کند. اساس کار کنگره ۶۰ بر محبت، عقل و ایمان استوار است. در جلسات، هر عضو از تجربه و دانش خودش در مقولهٔ اعتیاد و دستور جلسه سخن می‌گوید و از نصیحت کردن و شعار دادن پرهیز می‌شود. کنگره به هیچ گروهی وابسته نیست و از نظر مالی به خود متکی است؛ استحکام پایه‌های مالی و علمی، همواره از اهداف آن بوده است.",
  },
  {
    id: "lib-03",
    title: "راهنمای تازه‌واردین",
    sub: "راهنما • نخستین روزهای سفر •",
    color: NAV_COLORS.exam,
    text: "راهنمای تازه‌واردین، نخستین نقشهٔ راه برای روزهای اول درمان است. پنج نکتهٔ مهم: ۱) اعتیاد یک بیماری است و درمان دارد؛ برای درمان باید حتماً یک راهنما انتخاب کنی. ۲) نظم، پایهٔ درمان است: خواب و بیداری مشخص، ورزش صبحگاهی، تغذیهٔ درست و حضور به‌موقع در جلسات. ۳) مقدار و ساعت مصرف شربت OT را هیچ‌گاه بدون هماهنگی با راهنما تغییر نده. ۴) در سفر اول، مصرف طبق جدول پروتکل پله‌پله کاهش می‌یابد؛ صبر و پیگیری، کلید عبور از این مرحله است. ۵) سفر دوم بعد از رهایی از مواد آغاز می‌شود: بازسازی شخصیت و شناخت خود. و بدان که در این مسیر تنها نیستی؛ راهنما و همسفران کنار تو هستند. ما می‌توانیم.",
  },
  {
    id: "lib-04",
    title: "چهارده وادی عشق",
    sub: "کتاب • ۱۴ وادی • متن و صوت •",
    color: NAV_COLORS.journal,
    text: "کتاب «چهارده وادی عشق» (کتاب عشق) نوشتهٔ مهندس حسین دژاکام است؛ کتابِ سفر دوم، یعنی مرحلهٔ شناخت خویش که بعد از رهایی از مصرف مواد آغاز می‌شود. ترک مواد، نیمی از راه است و نیمهٔ دیگر، ساختن دوبارهٔ انسان. این کتاب این نیمهٔ دوم را در چهارده ایستگاه (وادی) شرح می‌دهد: وادی اول «آغاز» می‌گوید همه‌چیز از یک تصمیم درست شروع می‌شود؛ وادی‌های میانی، تفکر، مسئولیت‌پذیری، صبر، تسلیم و صلح درون را می‌آموزند؛ و وادی پایانی، انسان را به عشق‌ورزی و خدمت به دیگران می‌رساند. هر وادی یک درس برای زندگی سالم است؛ خواندن آن برای مسافران و همسفران هر دو آموزنده است.",
  },
  {
    id: "lib-05",
    title: "جهان‌بینی",
    sub: "جهان‌بینی ۱ و ۲ • تفکر درست، زندگی درست •",
    color: NAV_COLORS.plan,
    text: "جهان‌بینی، عینکی است که با آن دنیا را می‌بینیم؛ اگر تفکر ما بگوید «مشکل من حل نمی‌شود»، همین را ساخته‌ایم و مشکل حل نخواهد شد؛ اگر بگوید «بیماری من درمان دارد»، راه درمان باز می‌شود. کتاب‌های جهان‌بینی ۱ و ۲ می‌آموزند که پیش از هر تصمیم باید تفکر کرد؛ تفکر، انتخاب می‌آورد و انتخاب، سرنوشت را می‌سازد. با شناخت خود، ایمان به قدرت مطلق و ساختن تصویر مثبت از آینده، زندگی سالم ساخته می‌شود. برای همین در کنگره ۶۰ کلاس‌های جهان‌بینی، ادامهٔ درمان‌اند: درمان فقط دارو نیست؛ فکرِ سالم هم درمان است.",
  },
  {
    id: "dst",
    title: "پروتکل DST",
    sub: "متد درمان اعتیاد با روش DST.",
    color: NAV_COLORS.home,
    text: "DST مخفف «تخفیف تدریجی مصرف» است: هر ۱ سی‌سی شربت OT معادل ۱۰۰ میلی‌گرم تریاک خوراکی است؛ سه وعده در ساعت‌های ثابت با فاصله ۸ ساعت؛ هر پله ۲۱ روز و کاهش با ضریب ۰٫۸؛ توقف روی سقف مصرف ۲۲ روز؛ و پله آخر ۰٫۳cc در ۲۴ ساعت. جدول کامل پله‌ها در تب «پروتکل» برنامه است.",
  },
  {
    id: "vazayef",
    title: "شرح وظایف",
    sub: "وظایف مرزبانان و خدمتگزاران لژیون.",
    color: NAV_COLORS.settings,
    text: "لژیون مرزبانان نمایندگی همسفران آقا متشکل از ۵ نفر است: نگهبان، دبیر، استاد جلسه، مرزبان امور مالی و مهماندار. طول مدت خدمت مرزبانی ۶ ماه است و برگزاری انتخابات مرزبانی بر عهده راهنمای لژیون مرزبانی نمایندگی می‌باشد. وظیفهٔ هر یک در متن «شرح وظایف» کنگره ۶۰ آمده است.",
  },
  {
    id: "ghavanin",
    title: "قوانین کنگره ۶۰",
    sub: "حرمت‌ها و قوانین مجموعه.",
    color: NAV_COLORS.lib,
    text: "کنگره ۶۰ چهارده قانون دارد؛ چند نمونه: کنگره تشکیلات تحقیقاتی مستقلی است که با راهنمایی نیروهای ماوراءالطبیعه و توکل به قدرت مطلق حرکت می‌کند؛ شرط عضویت، پذیرفتن اصول و قوانین و تعهد به رهایی از دام اعتیاد است؛ گردانندگان از بین مسافران و همسفران انتخاب می‌شوند؛ به همهٔ خواهانِ رهایی، مساوات اطلاق می‌گردد؛ منابع مالی، کمک‌های داوطلبانهٔ اعضاست؛ و کنگره در موضوعات خارج از اهدافش نظری ندارد. حرمت‌ها هم جزو قوانین‌اند: در جلسات دروغ، غیبت، بحث سیاسی، قطع حرف دیگران، تلفن بدون مجوز راهنما و دریافت و پرداخت پول بدون هماهنگی ممنوع است. رعایت قوانین، پشتوانهٔ آرامش و امنیت جمع است.",
  },
  {
    id: "shast",
    title: "۶۰ درجه",
    sub: "کتاب ۶۰ درجه.",
    color: NAV_COLORS.journal,
    text: "کتاب «۶۰ درجه» روایت سفر مهندس حسین دژاکام و همراهانش برای عبور از «منطقهٔ ۶۰ درجه زیر صفر» — یعنی پایین‌ترین نقطهٔ زندگیِ مصرف — تا بازگشت به زندگی سالم است. نام کتاب از این ایده گرفته شده که گاهی با تغییر ۶۰ درجه‌ای زاویهٔ نگاه، همان مسئلهٔ قبلی معنای دیگری پیدا می‌کند. این کتاب با قصه‌گویی و مثال‌های ساده، مثلث تفکر، تصمیم و اجرا را آموزش می‌دهد و نشان می‌دهد انسان با شناخت خود و ایمان به قدرت مطلق، از هر نقطهٔ تاریکی می‌تواند بازگردد.",
  },
  {
    id: "behtarin",
    title: "بهترین راه",
    sub: "سخنان آموزشی کنگرهٔ ۶۰.",
    color: NAV_COLORS.plan,
    text: "جزوهٔ «بهترین راه» پیامی روشن دارد: بسیاری تصور می‌کنند بهترین راه، کوتاه‌ترین راه است و به درستی یا نادرستی آن توجه نمی‌کنند؛ راهی را می‌روند که زودتر به خواسته‌هایشان برساند؛ برای همین مشق‌هایشان ناتمام و بی‌محتوا می‌ماند و مجبورند دوباره بنویسند. راه نیمه‌تمام، مثل غذای نیمه‌پخته یا خام است: قابل خوردن نیست. در درمان اعتیاد هم راه‌های میان‌بُر به بازگشتِ مصرف ختم می‌شوند؛ راه درست، راهِ اصولی درمان است: پذیرش بیماری، درمان با متد DST زیر نظر راهنما، نظم و پیگیری. نتیجهٔ این راه، ماندگار است.",
  },
  {
    id: "mosalas",
    title: "مثلث‌های آموزشی",
    sub: "سه‌گانه‌های آموزشی درمان.",
    color: NAV_COLORS.exam,
    text: "مثلث‌های آموزشی کنگره ۶۰، سه‌گانه‌های پایهٔ درمان‌اند: تفکر، تصمیم، اجرا — شناخت، نظم، پیگیری — و آموزش، محبت، کاریابی. هر مثلث، سه ضلع دارد که اگر یکی ضعیف شود، تعادل کل مثلث به هم می‌ریزد؛ مسیر درمان، تمرین مداومِ حفظ تعادل این مثلث‌هاست.",
  },
  {
    id: "masraf",
    title: "مصرف‌کننده کیست؟",
    sub: "شناخت بیماری اعتیاد.",
    color: NAV_COLORS.home,
    text: "مصرف‌کنندهٔ مواد مخدر، فردی بیمار است که در اثر استفادهٔ مکرر از مواد، در ساختار وجودی خود تخریب پدید آورده و زندگی خود و اطرافیانش را آشفته کرده است؛ چه شروع مصرف دانسته باشد و چه ندانسته، فرقی نمی‌کند، چون هر دو نشانهٔ عدم آگاهی است. این تخریب در جسم، سیستم ضربان قلب، سیستم ایمنی و تولید مخدرهای طبیعی بدن را مختل می‌کند و در روان و جهان‌بینی، جهانی مملو از ترس، اضطراب، نگرانی و حقارت می‌سازد. برای اصلاح ۹۰٪ از این ساخته‌ها، حداقل ۱۰ ماه درمان لازم است. شناخت این واقعیت، اولین قدم درمان است: اعتیاد بیماری است، نه لذت‌جویی؛ و بیماری، درمان دارد.",
  },
];

const QUOTES = [
  "ما می‌توانیم؛ کافیست بخواهیم و تلاش کنیم.",
  "راه سخت است، اما رسیدن به آرامش، ارزش هر قدم را دارد.",
  "هر روز پاک، یک پیروزی بزرگ است.",
  "نظم و صبر، دو بال پرواز به سوی سلامتی‌اند.",
  "درمان یک سفر است؛ قدم به قدم و با عشق.",
  "با ورزش، نظم و حضور در جلسات، سفرت را محکم‌تر کن.",
];

const MOODS = ["😞", "😕", "😐", "🙂", "😄"];

type JournalEntry = { id: string; day: number; doseTaken: boolean[]; mood: number; note: string };

/* ================================ اجزای رابط ================================ */
type TabId = "home" | "plan" | "journal" | "exam" | "lib" | "settings";

const TABS: { id: TabId; label: string; color: string; Icon: React.ElementType }[] = [
  { id: "home", label: "خانه", color: NAV_COLORS.home, Icon: HomeIcon },
  { id: "plan", label: "پروتکل", color: NAV_COLORS.plan, Icon: CalendarDays },
  { id: "journal", label: "ثبت روزانه", color: NAV_COLORS.journal, Icon: NotebookPen },
  { id: "exam", label: "آزمون", color: NAV_COLORS.exam, Icon: FileQuestion },
  { id: "lib", label: "کتابخانه", color: NAV_COLORS.lib, Icon: BookOpen },
  { id: "settings", label: "تنظیمات", color: NAV_COLORS.settings, Icon: SettingsIcon },
];

function SectionCard(props: {
  c: ThemeColors;
  title?: string;
  children: React.ReactNode;
  className?: string;
  accent?: string;
}) {
  return (
    <div
      className={`rounded-[20px] p-4 shadow-sm ${props.className ?? ""}`}
      style={{ background: props.c.surface }}
    >
      {props.title && (
        <p className="mb-2.5 text-[15px] font-bold" style={{ color: props.accent ?? props.c.primary }}>
          {props.title}
        </p>
      )}
      <div className="flex flex-col gap-2.5">{props.children}</div>
    </div>
  );
}

function NoticeCard({ c, text }: { c: ThemeColors; text: string }) {
  return (
    <div
      className="flex items-start gap-2 rounded-[18px] p-3"
      style={{ background: c.tertiaryContainer, color: c.onTertiaryContainer }}
    >
      <span className="text-[15px] leading-6">⚠️</span>
      <p className="text-[11px] leading-5" style={{ textAlign: "justify" }}>{text}</p>
    </div>
  );
}

/** زمان جاری بعد از mount (برای جلوگیری از ناهم‌خوانی SSR) */
function useNow(intervalMs = 60000): number | null {
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    const tick = () => setNow(Date.now());
    const t = setTimeout(tick, 0);
    const iv = intervalMs > 0 ? setInterval(tick, intervalMs) : undefined;
    return () => {
      clearTimeout(t);
      if (iv) clearInterval(iv);
    };
  }, [intervalMs]);
  return now;
}

function WeCanBadge({ c }: { c: ThemeColors }) {
  return (
    <span
      className="rounded-full px-3 py-1.5 text-[12px] font-bold"
      style={{ background: c.tertiaryContainer, color: c.tertiary }}
    >
      ما می‌توانیم ✋
    </span>
  );
}

function ScreenHeader({ c, title, subtitle, accent }: { c: ThemeColors; title: string; subtitle?: string; accent?: string }) {
  return (
    <div className="mb-1">
      <h2 className="text-[22px] font-black" style={{ color: accent ?? c.primary }}>
        {title}
      </h2>
      {subtitle && (
        <p className="mt-1 text-[12.5px] leading-6" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
          {subtitle}
        </p>
      )}
    </div>
  );
}

function ChipIcon({ color, Icon, size = 36 }: { color: string; Icon: React.ElementType; size?: number }) {
  return (
    <span
      className="grid shrink-0 place-items-center rounded-full"
      style={{ width: size, height: size, background: `${color}26` }}
    >
      <Icon size={size * 0.53} color={color} />
    </span>
  );
}

/* ================================ صفحهٔ خانه ================================ */
function HomeScreen(props: {
  c: ThemeColors;
  settings: UserSettings;
  entries: JournalEntry[];
  go: (t: TabId) => void;
}) {
  const { c, settings, entries, go } = props;
  const plan = useMemo(() => buildPlan(settings), [settings]);
  const now = useNow();
  if (now == null) return null;

  const days = daysSinceStart(settings, now);
  const stepIndex = currentStepIndex(plan, days);
  const step = stepIndex != null ? plan.steps[stepIndex] : null;
  const times = doseTimes(settings.doseModel);
  const progress = plan.steps.reduce((s, st) => s + st.days, 0) > 0
    ? Math.min(Math.max((days + 1) / plan.steps.reduce((s, st) => s + st.days, 0), 0), 1)
    : null;
  const dNow = new Date(now);
  const dayOfYear = Math.floor((dNow.getTime() - new Date(dNow.getFullYear(), 0, 0).getTime()) / 86400000);
  const quote = QUOTES[dayOfYear % QUOTES.length];
  const todayEntry = entries.find((e) => e.day === normalizeDay(now));

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-[18px] font-black" style={{ color: c.onBg }}>
            سلام مسافر 👋
          </p>
          <p className="mt-0.5 text-[12px]" style={{ color: c.onSurfaceVariant }}>
            {settings.startDate != null ? `امروز روز ${faNum(days + 1)} سفر توست` : "سفرت هنوز ثبت نشده است"}
          </p>
        </div>
        <WeCanBadge c={c} />
      </div>

      {settings.startDate == null ? (
        <div className="rounded-[28px] p-5 text-white" style={{ background: `linear-gradient(120deg, ${NAV_COLORS.home}, ${HOME_HERO_PINK})` }}>
          <p className="text-[18px] font-black">سفرت را شروع کن!</p>
          <p className="mt-1.5 text-[12px] leading-6 opacity-90" style={{ textAlign: "justify" }}>
            برای فعال شدن برنامه DST، اول نوع مادهٔ مصرفی، تاریخ شروع و مقدار مصرف را در تنظیمات ثبت کن.
          </p>
          <button
            onClick={() => go("settings")}
            className="mt-4 flex items-center gap-1.5 rounded-full bg-white/20 px-4 py-2 text-[13px] font-bold text-white active:scale-[0.98]"
          >
            <SettingsIcon size={15} /> ثبت اطلاعات در تنظیمات
          </button>
        </div>
      ) : (
        <>
          <div className="rounded-[28px] p-5 text-white" style={{ background: `linear-gradient(120deg, ${NAV_COLORS.home}, ${HOME_HERO_PINK})` }}>
            <p className="text-[12px] opacity-90">روز پاک من</p>
            <p className="mt-1 text-[38px] font-black leading-none">{faNum(days + 1)}</p>
            <p className="mt-2 text-[12px] opacity-90">از رهایی تا رهایی — متد DST</p>
            <div className="mt-4 flex gap-2.5">
              <button
                onClick={() => go("exam")}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-full bg-white/20 py-2.5 text-[13px] font-bold active:scale-[0.98]"
              >
                <FileQuestion size={15} /> شروع آزمون
              </button>
              <button
                onClick={() => go("journal")}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-full bg-white/20 py-2.5 text-[13px] font-bold active:scale-[0.98]"
              >
                <NotebookPen size={15} /> ثبت روزانه
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2.5">
            {[
              { label: "روز سفر", value: faNum(days + 1), color: NAV_COLORS.home, Icon: CalendarDays },
              { label: "پلهٔ جاری", value: step ? step.title : "پایان", color: NAV_COLORS.plan, Icon: Flag },
              { label: "هر وعده", value: step && step.perDoseCc > 0 ? `${faDose(step.perDoseCc)}cc` : "—", color: NAV_COLORS.journal, Icon: Droplet },
              { label: "پیشرفت سفر", value: progress != null ? `${faNum(Math.round(progress * 100))}٪` : "—", color: HOME_HERO_PINK, Icon: TrendingUp },
            ].map((s) => (
              <div key={s.label} className="flex items-center gap-2.5 rounded-[20px] p-3 shadow-sm" style={{ background: c.surface }}>
                <ChipIcon color={s.color} Icon={s.Icon} />
                <div className="min-w-0">
                  <p className="truncate text-[14.5px] font-bold" style={{ color: c.onBg }}>
                    {s.value}
                  </p>
                  <p className="text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
                    {s.label}
                  </p>
                </div>
              </div>
            ))}
          </div>

          <SectionCard c={c} title="وضعیت درمان" accent={NAV_COLORS.home}>
            {progress != null && (
              <>
                <div className="h-2 overflow-hidden rounded-full" style={{ background: c.surfaceVariant }}>
                  <div className="h-full rounded-full transition-all" style={{ width: `${progress * 100}%`, background: NAV_COLORS.home }} />
                </div>
                <p className="text-[11px]" style={{ color: c.onSurfaceVariant }}>
                  پیشرفت سفر: {faNum(Math.round(progress * 100))}٪ از کل {faNum(plan.steps.reduce((s, st) => s + st.days, 0))} روز
                </p>
              </>
            )}
            <p className="text-[12px]" style={{ color: c.onSurfaceVariant }}>
              تاریخ شروع: {jalaliLong(settings.startDate)} · {SUBSTANCES.find((s) => s.id === settings.substance)?.label}
            </p>
          </SectionCard>

          {step ? (
            <SectionCard c={c} title="نوبت‌های امروز" accent={NAV_COLORS.home}>
              {times.slice(0, Math.min(Math.max(step.dosesPerDay, 1), 3)).map((t, i) => {
                const done = todayEntry?.doseTaken[i] ?? false;
                return (
                  <div key={t} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {done ? (
                        <CheckCircle2 size={18} color={NAV_COLORS.plan} />
                      ) : (
                        <Clock size={18} color={NAV_COLORS.lib} />
                      )}
                      <span className="text-[12.5px]" style={{ color: c.onBg }}>
                        {t}
                      </span>
                    </div>
                    <span className="text-[12.5px] font-bold" style={{ color: done ? NAV_COLORS.plan : c.primary }}>
                      {done ? `${faDose(step.perDoseCc)}cc مصرف شد` : `${faDose(step.perDoseCc)}cc باقی است`}
                    </span>
                  </div>
                );
              })}
              {step.note && (
                <p className="text-[10.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                  {step.note}
                </p>
              )}
            </SectionCard>
          ) : (
            <SectionCard c={c} title="پایان برنامه" accent={NAV_COLORS.home}>
              <p className="text-[12.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
                همه پله‌های این برنامه طی شده است. طبق جزوه، پله آخر (۰٫۳cc در ۲۴ ساعت) ۲ تا ۲۱ روز ادامه دارد و پس از دیدن شرایط پله آخر، مصرف با نظر راهنما قطع می‌شود. خدا قوت — سفر دوم آغاز شد. 🌱
              </p>
            </SectionCard>
          )}

          <button
            onClick={() => go("exam")}
            className="flex items-center gap-3 rounded-[20px] p-3.5 text-right shadow-sm active:scale-[0.99]"
            style={{ background: c.surface }}
          >
            <ChipIcon color={NAV_COLORS.plan} Icon={FileQuestion} size={40} />
            <div className="flex-1">
              <p className="text-[13.5px] font-bold" style={{ color: c.onBg }}>
                آزمون امروزت رو بده!
              </p>
              <p className="text-[11px]" style={{ color: c.onSurfaceVariant }}>
                ۱۰ سؤال — چند دقیقه بیشتر وقت نمی‌گیره
              </p>
            </div>
            <ChevronLeft size={18} color={c.outline} />
          </button>
        </>
      )}

      <SectionCard c={c}>
        <p className="truncate text-center text-[13.5px] font-medium" style={{ color: c.onSurfaceVariant }}>
          «{quote}»
        </p>
      </SectionCard>

      <NoticeCard
        c={c}
        text="این برنامه همراه آموزشی است و به‌هیچ‌وجه جایگزین راهنما و لژیون نیست. هر تغییری در پله‌ها فقط با هماهنگی راهنمای خود انجام بده."
      />
    </div>
  );
}

/* ================================ صفحهٔ پروتکل ================================ */
function ProtocolScreen({ c, settings }: { c: ThemeColors; settings: UserSettings }) {
  const plan = useMemo(() => buildPlan(settings), [settings]);
  const now = useNow(0);
  const days = now != null ? daysSinceStart(settings, now) : 0;
  const currentIndex = now != null ? currentStepIndex(plan, days) : null;
  const sub = SUBSTANCES.find((s) => s.id === settings.substance)!;
  const totalDays = plan.steps.reduce((s, st) => s + st.days, 0);
  const usesOtLadder = settings.substance !== "STIMULANT" && settings.substance !== "ALCOHOL";

  /* پلهٔ جاری داخل جدول ۱۵ پله‌ای OT — برای تیک ✓ و کاشی فیروزه‌ای */
  const currentStep = currentIndex != null ? plan.steps[currentIndex] : null;
  const currentLadderIdx = currentStep ? OT_LADDER.findIndex((s) => s.title === currentStep.title) : -1;
  const stepStart = currentIndex != null ? stepStartDay(plan, currentIndex) : 0;
  const stepEnd = currentStep ? stepStart + currentStep.days : 0;
  const remainDays = currentStep ? Math.max(1, stepEnd - days) : 0;
  const hasStart = settings.startDate != null;

  /* پلهٔ انتخاب‌شده با کلیک — هایلایت کاشی + نمایش توضیح در کارت بالا (مطابق عکس ۹) */
  const [selStep, setSelStep] = useState<number | null>(null);
  const ladderStartDay = (i: number) => OT_LADDER.slice(0, i).reduce((s, st) => s + st.days, 0);
  const ladderTotal = OT_LADDER.reduce((s, st) => s + st.days, 0);
  const pickStep = (i: number) => {
    setSelStep((prev) => (prev === i ? null : i));
    document.getElementById("app-scroll")?.scrollTo({ top: 0, behavior: "smooth" });
  };

  /* محتوای کارت بالا: اگر پله‌ای انتخاب شده باشد توضیح همان پله، وگرنه پلهٔ جاری */
  const viewStep = selStep != null ? OT_LADDER[selStep] : currentStep;
  const viewTitle =
    selStep != null
      ? `پلهٔ ${faNum(selStep + 1)} از ${faNum(OT_LADDER.length)} — روز ${faNum(ladderStartDay(selStep) + 1)} تا ${faNum(ladderStartDay(selStep) + OT_LADDER[selStep].days)} سفر`
      : currentStep
        ? `${currentStep.title} — روز ${faNum(stepStart + 1)} تا ${faNum(stepEnd)} سفر`
        : "برای دیدن پلهٔ جاری، تاریخ شروع درمان را در تنظیمات ثبت کن";
  const viewNote = selStep != null ? OT_LADDER[selStep].note : "هر پله ۲۱ روز • کاهش ۲۰٪ مصرف نسبت به پلهٔ قبل";
  const viewProgress =
    selStep != null
      ? (ladderStartDay(selStep) / ladderTotal) * 100
      : totalDays > 0
        ? Math.min(100, Math.max(2, (days / totalDays) * 100))
        : 0;
  const viewDose = selStep != null ? OT_LADDER[selStep] : currentStep;
  const viewEndDays = selStep != null ? ladderStartDay(selStep) + OT_LADDER[selStep].days : stepEnd;

  return (
    <div className="flex flex-col gap-3">
      <ScreenHeader
        c={c}
        title="پروتکل"
        subtitle="پله‌های کاهش مصرف — متد DST (اصلاحیهٔ ۱۳۹۸/۳/۱)"
        accent={NAV_COLORS.plan}
      />

      {/* کارت بالای درمان — با کلیک روی هر پله، توضیح همان پله همین‌جا نمایش داده می‌شود */}
      <SectionCard c={c} accent={NAV_COLORS.plan}>
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-[14.5px] font-black" style={{ color: c.onBg }}>{viewTitle}</p>
            <p className="mt-0.5 text-[11px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
              {viewNote}
            </p>
          </div>
          <span
            className="grid h-12 w-12 shrink-0 place-items-center rounded-full"
            style={{ background: `${NAV_COLORS.plan}1f` }}
            aria-hidden="true"
          >
            <Flag size={22} color={NAV_COLORS.plan} />
          </span>
        </div>
        <div className="mt-3 h-2 overflow-hidden rounded-full" style={{ background: c.surfaceVariant }}>
          <div className="h-full rounded-full transition-all duration-300" style={{ width: `${viewProgress}%`, background: NAV_COLORS.plan }} />
        </div>
        {viewDose && viewDose.perDoseCc > 0 && (
          <div className="mt-3 flex items-center gap-2 rounded-xl px-3 py-2.5" style={{ background: `${NAV_COLORS.plan}12` }}>
            <Droplet size={15} color={NAV_COLORS.plan} className="shrink-0" />
            <p className="text-[12.5px] font-bold" style={{ color: NAV_COLORS.plan }}>
              مصرف هر وعده: {faDose(viewDose.perDoseCc)}cc — {viewDose.dosesPerDay === 3 ? "سه" : viewDose.dosesPerDay === 2 ? "دو" : "یک"} وعده در شبانه‌روز
            </p>
          </div>
        )}
        {viewStep && hasStart && (
          <p className="mt-2 text-[11px] font-bold" style={{ color: AMBER }}>
            پایان این پله: {jalaliLong(settings.startDate + viewEndDays * 86400000)}
          </p>
        )}
        {selStep != null && selStep !== currentLadderIdx && (
          <p className="mt-1 text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
            برای بازگشت به پلهٔ جاری، دوباره روی همان پله بزن.
          </p>
        )}
      </SectionCard>

      {/* پله‌ها — کاشی ۳تایی × ۵ ردیف (۱۵ پلهٔ DST) — با کلیک، پله هایلایت و توضیحش بالا نمایش داده می‌شود */}
      <div>
        <div className="mb-2 flex items-baseline justify-between">
          <p className="text-[12.5px] font-bold" style={{ color: c.onBg }}>پله‌ها</p>
          <p className="text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
            {faNum(OT_LADDER.length)} پله تا رهایی • هر پله ۲۱ روز
          </p>
        </div>
        <div className="flex flex-wrap justify-center gap-2">
          {OT_LADDER.map((step, i) => {
            const isCurrent = hasStart && i === currentLadderIdx;
            const isDone = hasStart && currentLadderIdx >= 0 && i < currentLadderIdx;
            const isSelected = selStep === i;
            const teal = isSelected || (selStep == null && isCurrent);
            return (
              <button
                key={step.title}
                onClick={() => pickStep(i)}
                aria-pressed={isSelected}
                aria-label={`نمایش توضیح ${step.title}`}
                className="flex w-[31%] flex-col items-center justify-center rounded-2xl py-3 active:scale-95"
                style={{
                  background: teal ? NAV_COLORS.plan : isDone ? `${GREEN_OK}1a` : isCurrent ? `${NAV_COLORS.plan}14` : c.surface,
                  border: `1px solid ${teal ? NAV_COLORS.plan : isDone ? `${GREEN_OK}55` : isCurrent ? NAV_COLORS.plan : c.outlineVariant}`,
                }}
              >
                {isDone && !isSelected ? (
                  <Check size={14} color={GREEN_OK} />
                ) : (
                  <span className="h-[14px]" aria-hidden="true" />
                )}
                <span className="text-[13px] font-black" style={{ color: teal ? "#FFFFFF" : c.onBg }}>
                  پلهٔ {faNum(i + 1)}
                </span>
                <span className="mt-0.5 text-[9.5px]" style={{ color: teal ? "#FFFFFF" : c.onSurfaceVariant }}>
                  {isSelected && isCurrent
                    ? `${faNum(remainDays)} روز`
                    : step.kind === "FINAL"
                      ? "۲ تا ۲۱ روز"
                      : `${faNum(step.days)} روز`}
                </span>
              </button>
            );
          })}
        </div>
        <p className="mt-1.5 text-center text-[10px]" style={{ color: c.onSurfaceVariant }}>
          جدول تیپرینگ OT — پیوست ۱ (۱۵ پله) • با کلیک روی هر پله، توضیح آن بالا نمایش داده می‌شود
        </p>
      </div>

      <SectionCard c={c} title="مشخصات برنامه" accent={NAV_COLORS.plan}>
        {[
          ["نوع ماده", sub.label],
          ["برنامه اجرایی", sub.program],
          ["OT روزانه در پله اول", `${faDose(plan.startDailyCc)} سی‌سی`],
          ["سقف OT روزانه", `${faDose(plan.capDailyCc)} سی‌سی`],
          ["طول کل برنامه", `${faNum(totalDays)} روز (حدود ${faNum(Math.floor(totalDays / 30.4) + 1)} ماه)`],
        ].map(([k, v]) => (
          <div key={k} className="flex items-center justify-between">
            <span className="text-[12.5px]" style={{ color: c.onBg }}>
              {k}
            </span>
            <span className="text-[12.5px] font-bold" style={{ color: NAV_COLORS.plan }}>
              {v}
            </span>
          </div>
        ))}
      </SectionCard>

      {usesOtLadder && (
        <SectionCard c={c} title="توضیح جدول OT" accent={NAV_COLORS.plan}>
          {OT_LADDER_NOTES.map((n) => (
            <p key={n} className="text-[11px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
              • {n}
            </p>
          ))}
        </SectionCard>
      )}

      <NoticeCard
        c={c}
        text="واحد درمان: شربت اپیوم تینکچر (سی‌سی). ورود به جدول کاهش و تغییر پله‌ها فقط با تشخیص راهنما انجام می‌شود؛ تکرار پله در کل سفر فقط یک‌بار و افزایش پله حداکثر تا ۲ پله مجاز است."
      />

      <SectionCard c={c}>
        <p className="text-[11px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
          فاصله وعده‌ها ۸ ساعت است (مثلاً ۶ صبح، ۲ بعدازظهر و ۱۰ شب — مدل ۲). ساعت وعده شب بنا به تشخیص راهنما می‌تواند تا یک ساعت پایین‌تر بیاید.
        </p>
      </SectionCard>
    </div>
  );
}

/* ================================ ثبت روزانه ================================ */
function JournalScreen(props: {
  c: ThemeColors;
  settings: UserSettings;
  entries: JournalEntry[];
  setEntries: (e: JournalEntry[]) => void;
}) {
  const { c, settings, entries, setEntries } = props;
  const [showDialog, setShowDialog] = useState(false);
  const plan = useMemo(() => buildPlan(settings), [settings]);
  const now = useNow(0);
  if (now == null) return null;

  const days = daysSinceStart(settings, now);
  const stepDoses = currentStepIndex(plan, days) != null ? plan.steps[currentStepIndex(plan, days) as number].dosesPerDay : 3;
  const todayEntry = entries.find((e) => e.day === normalizeDay(now));

  return (
    <div className="flex flex-col gap-3">
      <ScreenHeader c={c} title="ثبت روزانه" subtitle="هر شب چند دقیقه وقت بگذار؛ نگاه به گذشته، چراغ راه آینده است" accent={NAV_COLORS.journal} />

      <button
        onClick={() => setShowDialog(true)}
        className="w-full rounded-xl py-3 text-[13.5px] font-bold active:scale-[0.99]"
        style={{ background: NAV_COLORS.journal, color: "#FFFFFF" }}
      >
        {todayEntry ? "ویرایش ثبت امروز" : "ثبت وضعیت امروز"}
      </button>

      {entries.length === 0 && (
        <SectionCard c={c}>
          <p className="text-[12.5px] leading-6" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
            هنوز چیزی ثبت نشده. بعد از نوبت مصرف شب، حالت را ثبت کن تا مسیر پیشرفتت را ببینی.
          </p>
        </SectionCard>
      )}

      {[...entries]
        .sort((a, b) => b.day - a.day)
        .map((entry) => (
          <SectionCard c={c} key={entry.id}>
            <div className="flex items-center justify-between">
              <p className="text-[13px] font-bold" style={{ color: c.onBg }}>
                {jalaliLong(entry.day)}
              </p>
              <button
                onClick={() => setEntries(entries.filter((e) => e.id !== entry.id))}
                aria-label="حذف"
                className="grid h-9 w-9 place-items-center rounded-full active:scale-95"
              >
                <Trash2 size={17} color={RED_NO} />
              </button>
            </div>
            <div className="flex items-center gap-1.5">
              {Array.from({ length: Math.max(stepDoses, 1) }).map((_, i) => (
                <span key={i} className="text-[15px]">
                  {entry.doseTaken[i] ? "✅" : "⬜️"}
                </span>
              ))}
              <span className="ms-1 text-[15px]">{MOODS[entry.mood] ?? "😐"}</span>
            </div>
            {entry.note && (
              <p className="text-[12px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                {entry.note}
              </p>
            )}
          </SectionCard>
        ))}

      {showDialog && (
        <EntryDialog
          c={c}
          existing={todayEntry ?? null}
          doses={stepDoses}
          today={normalizeDay(now)}
          onClose={() => setShowDialog(false)}
          onConfirm={(e) => {
            setEntries([...entries.filter((x) => x.id !== e.id), e]);
            setShowDialog(false);
          }}
        />
      )}
    </div>
  );
}

function EntryDialog(props: {
  c: ThemeColors;
  existing: JournalEntry | null;
  doses: number;
  today: number;
  onClose: () => void;
  onConfirm: (e: JournalEntry) => void;
}) {
  const { c, existing, doses, today, onClose, onConfirm } = props;
  const [morning, setMorning] = useState(existing?.doseTaken[0] ?? false);
  const [noon, setNoon] = useState(existing?.doseTaken[1] ?? false);
  const [night, setNight] = useState(existing?.doseTaken[2] ?? false);
  const [mood, setMood] = useState(existing?.mood ?? 2);
  const [note, setNote] = useState(existing?.note ?? "");

  return (
    <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className="w-full rounded-[22px] p-4 shadow-xl" style={{ background: c.surface }} onClick={(e) => e.stopPropagation()}>
        <p className="text-[15px] font-bold" style={{ color: c.primary }}>
          ثبت امروز
        </p>
        <p className="mt-3 text-[12.5px] font-bold" style={{ color: c.onBg }}>
          نوبت‌های مصرف‌شده:
        </p>
        {[
          ["صبح", morning, setMorning] as const,
          ["ظهر", noon, setNoon] as const,
          ["شب", night, setNight] as const,
        ].map(([label, val, set]) => (
          <label key={label} className="mt-2 flex w-fit items-center gap-2">
            <input
              type="checkbox"
              checked={val}
              onChange={(e) => set(e.target.checked)}
              className="h-4 w-4"
              style={{ accentColor: c.primary }}
            />
            <span className="text-[12.5px]" style={{ color: c.onBg }}>
              {label}
            </span>
          </label>
        ))}
        <p className="mt-3 text-[12.5px] font-bold" style={{ color: c.onBg }}>
          حالت امروز چطور بود؟
        </p>
        <div className="mt-1 flex gap-1.5">
          {MOODS.map((m, i) => (
            <button
              key={m}
              onClick={() => setMood(i)}
              className="grid h-10 w-10 place-items-center rounded-xl text-[20px] active:scale-95"
              style={{ background: mood === i ? c.container : "transparent" }}
              aria-label={`حالت ${i + 1}`}
            >
              {m}
            </button>
          ))}
        </div>
        <textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="یادداشت (اختیاری)"
          rows={2}
          className="mt-3 w-full rounded-xl border p-2.5 text-[12.5px] outline-none"
          style={{ borderColor: c.outlineVariant, background: c.bg, color: c.onBg }}
        />
        <p className="mt-1 text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
          تاریخ: {jalaliLong(today)}
        </p>
        <div className="mt-3 flex gap-2">
          <button
            onClick={() =>
              onConfirm({
                id: String(today),
                day: today,
                doseTaken: [morning, noon, night],
                mood,
                note,
              })
            }
            className="flex-1 rounded-xl py-2.5 text-[13px] font-bold active:scale-[0.98]"
            style={{ background: c.primary, color: c.onPrimary }}
          >
            ذخیره
          </button>
          <button
            onClick={onClose}
            className="flex-1 rounded-xl border py-2.5 text-[13px] font-bold active:scale-[0.98]"
            style={{ borderColor: c.outline, color: c.onBg, background: c.surface }}
          >
            انصراف
          </button>
        </div>
      </div>
    </div>
  );
}

/* ================================ آزمون ================================ */
/* پنج کارت اصلی صفحهٔ شروع آزمون — طرح مصوب «عکس خوب» + حفظ امکانات فعلی */
const QUIZ_LENS = [10, 20, 30, 40, 50, 60, 70, 100];
type ExamCardId = "term" | "cat" | "quick" | "review" | "written";
const EXAM_CARDS: { id: ExamCardId; icon: typeof BookOpen; color: string; title: string; sub: string }[] = [
  { id: "term", icon: ClipboardList, color: "#F97316", title: "آزمون ترم", sub: "۱۰۰ سؤال · ۴۵ دقیقه — استاندارد جلسهٔ ترم" },
  { id: "cat", icon: Layers, color: "#14B8A6", title: "تمرین دسته‌ای", sub: "تمرین روی یک منبع — مثلاً فقط ۱۴ وادی" },
  { id: "quick", icon: Timer, color: "#F59E0B", title: "آزمون سریع", sub: "۵ سؤال — چند دقیقه وقت می‌برد" },
  { id: "review", icon: RotateCcw, color: "#EC4899", title: "مرور اشتباه‌ها", sub: "فقط سؤال‌هایی که غلط جواب دادی" },
  { id: "written", icon: BookOpen, color: "#E11D48", title: "تمرین تشریحی", sub: "سؤال‌های متن کوتاه با پاسخ — اول خودت جواب بده، بعد پاسخ را ببین" },
];

/* منابع جزوه‌ای کنگره ۶۰ — سؤالاتشان به بانک اضافه شده و در چیپ‌های «انتخاب منبع» فعال‌اند */

function ExamScreen({ c }: { c: ThemeColors }) {
  const [bank, setBank] = useState<Bank | null>(null);
  const [bankError, setBankError] = useState(false);
  const [mode, setMode] = useState<"quiz" | "flash">("quiz");
  const [phase, setPhase] = useState<"start" | "running" | "done">("start");
  const [quizList, setQuizList] = useState<QuizItem[]>([]);
  const [qIndex, setQIndex] = useState(0);
  const [chosen, setChosen] = useState<number | null>(null);
  const [answered, setAnswered] = useState(false);
  const [quizPicks, setQuizPicks] = useState<(number | null)[]>([]);
  const [quizLen, setQuizLen] = useState(10);
  const [flashList, setFlashList] = useState<BankQa[]>([]);
  const [fIndex, setFIndex] = useState(0);
  const [reveal, setReveal] = useState(false);
  const [flashScores, setFlashScores] = useState<(boolean | null)[]>([]);
  const [quizCats, setQuizCats] = useState<Set<string>>(new Set());
  const [flashCats, setFlashCats] = useState<Set<string>>(new Set());
  /* فقط سؤال‌های متن کوتاه در تمرین تشریحی (پیش‌فرض) — بلندها با گزینهٔ «همه» */
  const [flashShortOnly, setFlashShortOnly] = useState(true);
  const [noKeyMsg, setNoKeyMsg] = useState(false);
  /* کارت انتخاب‌شدهٔ صفحهٔ شروع (طرح ۵ کارت) + ذخیره‌گاه غلط‌ها */
  const [card, setCard] = useState<null | "term" | "cat" | "quick" | "review" | "written">(null);
  const [isReview, setIsReview] = useState(false);
  const [wrongCount, setWrongCount] = useState(0);
  const [reviewEmpty, setReviewEmpty] = useState(false);

  useEffect(() => {
    fetch("/questionbank.json?v=3", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error("http"))))
      .then((d: Bank) => setBank(d))
      .catch(() => setBankError(true));
  }, []);

  const bankMcqUsable = useMemo(() => {
    if (!bank) return [];
    return bank.mcq.filter((m) => {
      if (!m.opts || typeof m.opts !== "object" || !m.ans) return false;
      const valid = LETTER_ORDER.filter((k) => (m.opts[k] ?? "").trim() !== "");
      return valid.length >= 2 && valid.includes(m.ans);
    });
  }, [bank]);
  const bankQaUsable = useMemo(() => {
    if (!bank) return [];
    return bank.qa.filter((x) => (x.q ?? "").trim().length >= 12 && (x.a ?? "").trim().length >= 10);
  }, [bank]);
  const quizCatOptions = useMemo(() => {
    /* همهٔ دسته‌های بانک (همهٔ PDFها) + هستهٔ متن DST — حتی اگر هنوز کلید نداشته باشند */
    const set: string[] = [];
    if (bank) for (const m of bank.mcq) if (!set.includes(m.cat)) set.push(m.cat);
    return [CORE_CAT, ...set];
  }, [bank]);
  const flashCatOptions = useMemo(() => {
    const set: string[] = [];
    for (const q of bankQaUsable) if (!set.includes(q.cat)) set.push(q.cat);
    return set;
  }, [bankQaUsable]);
  /* سؤال‌های متن کوتاه (پاسخ ≤ ۱۲۰ حرف) — برای مرور سریع تشریحی؛
     دسته‌های کتابی «۶۰ درجه» و «تصاویر کتاب ۶۰ درجه» معاف‌اند (پاسخ‌هایشان کتابی و بالای سقف است
     و اگر معاف نبودند، در حالت پیش‌فرض هیچ سؤالی برایشان پیدا نمی‌شد) */
  const QA_SHORT_MAX = 120;
  const ALWAYS_QA_CATS = new Set(["۶۰ درجه", "تصاویر کتاب ۶۰ درجه"]);
  const isShortQa = (q: BankQa) => ALWAYS_QA_CATS.has(q.cat) || (q.a ?? "").length <= QA_SHORT_MAX;
  const shortQaCount = useMemo(() => bankQaUsable.filter(isShortQa).length, [bankQaUsable]);

  /* ——— آزمون شبانه (پیش‌نمایش وب) ——— */
  const todayKey = () => {
    const d = new Date();
    return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
  };
  const [night, setNight] = useState<{ enabled: boolean; hour: number; minute: number }>({
    enabled: false,
    hour: 21,
    minute: 30,
  });
  const [nightDone, setNightDone] = useState(false);
  const [nightBanner, setNightBanner] = useState(false);
  const setNightExam = (patch: Partial<{ enabled: boolean; hour: number; minute: number }>) => {
    const next = { ...night, ...patch };
    setNight(next);
    try {
      localStorage.setItem("hamrah_night_exam", JSON.stringify(next));
    } catch {
      /* بی‌صدا */
    }
    if (next.enabled && "Notification" in window && Notification.permission === "default") {
      Notification.requestPermission();
    }
  };
  const markNightDone = () => {
    try {
      localStorage.setItem("hamrah_night_done", todayKey());
    } catch {
      /* بی‌صدا */
    }
    setNightDone(true);
  };
  useEffect(() => {
    const id = window.setTimeout(() => {
      try {
        const raw = JSON.parse(localStorage.getItem("hamrah_night_exam") ?? "null");
        if (raw && typeof raw.enabled === "boolean") setNight(raw);
        setNightDone(localStorage.getItem("hamrah_night_done") === todayKey());
      } catch {
        /* بی‌صدا */
      }
    }, 0);
    return () => window.clearTimeout(id);
  }, []);
  useEffect(() => {
    const iv = setInterval(() => {
      if (!night.enabled) return;
      const now = new Date();
      const due = new Date();
      due.setHours(night.hour, night.minute, 0, 0);
      let doneToday = false;
      let shownToday = false;
      try {
        doneToday = localStorage.getItem("hamrah_night_done") === todayKey();
        shownToday = localStorage.getItem("hamrah_night_shown") === todayKey();
      } catch {
        /* بی‌صدا */
      }
      if (now >= due && !doneToday && !shownToday) {
        try {
          localStorage.setItem("hamrah_night_shown", todayKey());
        } catch {
          /* بی‌صدا */
        }
        setNightBanner(true);
        if ("Notification" in window && Notification.permission === "granted") {
          try {
            new Notification("🌙 آزمون شبانه", {
              body: "امشب هنوز آزمون ندادی! چند دقیقه وقت بگذار — ما می‌توانیم ✋",
            });
          } catch {
            /* بی‌صدا */
          }
        }
      }
    }, 30000);
    return () => clearInterval(iv);
  }, [night]);

  const startQuiz = (opts?: { len?: number; cats?: Set<string>; review?: boolean }) => {
    if (!bank) return;
    if (opts?.review) {
      const stored = loadWrong();
      if (stored.length === 0) {
        setReviewEmpty(true);
        return;
      }
      setReviewEmpty(false);
      setIsReview(true);
      const list = shuffle(stored);
      setQuizList(list);
      setQuizPicks(new Array(list.length).fill(null));
      setQIndex(0);
      setChosen(null);
      setAnswered(false);
      setPhase("running");
      return;
    }
    setIsReview(false);
    const cats = opts?.cats ?? quizCats;
    const len = opts?.len ?? quizLen;
    const core =
      cats.size === 0 || cats.has(CORE_CAT)
        ? CORE_DST.map((q, i) => ({ ...q, id: `core-${String(i + 1).padStart(2, "0")}` }))
        : [];
    const fromBank = bankMcqUsable.filter((m) => cats.size === 0 || cats.has(m.cat)).map(mcqToQuiz);
    /* چرخش هوشمند: اول سؤال‌های تازه — سؤال‌های سشن قبل آخر صف می‌آیند */
    const ordered = rotateFresh(
      [...core, ...fromBank].map((q) => [q.id ?? q.question, q] as [string, QuizItem]),
      loadRecent("quiz"),
    );
    const list = ordered.slice(0, Math.min(len, ordered.length));
    saveRecent("quiz", list.map((q) => q.id ?? q.question));
    if (list.length === 0) {
      setNoKeyMsg(true);
      return;
    }
    setNoKeyMsg(false);
    setQuizList(list);
    setQuizPicks(new Array(list.length).fill(null));
    setQIndex(0);
    setChosen(null);
    setAnswered(false);
    setPhase("running");
  };
  const startFlash = () => {
    const idPool = bankQaUsable.filter(
      (q) => (flashCats.size === 0 || flashCats.has(q.cat)) && (!flashShortOnly || isShortQa(q)),
    );
    /* چرخش هوشمند: سؤال‌های تازه اول */
    const list = rotateFresh(idPool.map((q) => [q.id, q] as [string, BankQa]), loadRecent("flash"));
    saveRecent("flash", list.map((q) => q.id));
    setFlashList(list);
    setFlashScores(new Array(list.length).fill(null));
    setFIndex(0);
    setReveal(false);
    setPhase("running");
  };

  /* بازگشت / پیشروی گام‌به‌گام داخل آزمون (فلش‌های بالا) */
  const goQuiz = (i: number) => {
    const clamped = Math.max(0, Math.min(i, quizList.length - 1));
    setQIndex(clamped);
    setChosen(quizPicks[clamped] ?? null);
    setAnswered(quizPicks[clamped] != null);
  };
  const goFlash = (i: number) => {
    const clamped = Math.max(0, Math.min(i, flashList.length - 1));
    setFIndex(clamped);
    setReveal(flashScores[clamped] != null);
  };

  /* ذخیرهٔ سؤال‌های غلط — در حالت مرور، فقط غلط‌های دوباره باقی می‌مانند */
  const recordWrongs = () => {
    const wrongItems = quizList.filter((q, i) => quizPicks[i] != null && quizPicks[i] !== q.correctIndex);
    if (isReview) {
      saveWrong(wrongItems);
    } else {
      const merged = loadWrong();
      for (const w of wrongItems) if (!merged.some((x) => x.question === w.question)) merged.push(w);
      saveWrong(merged);
    }
    setWrongCount(loadWrong().length);
  };

  const quizScore = quizPicks.reduce((s, p, i) => (p != null && quizList[i] && p === quizList[i].correctIndex ? s + 1 : s), 0);
  const quizWrong = quizPicks.reduce((s, p, i) => (p != null && quizList[i] && p !== quizList[i].correctIndex ? s + 1 : s), 0);
  const known = flashScores.reduce((s, v) => (v === true ? s + 1 : s), 0);
  const unknown = flashScores.reduce((s, v) => (v === false ? s + 1 : s), 0);
  const history = quizList
    .map((q, i) => ({ q, chosenIndex: quizPicks[i] }))
    .filter((h): h is { q: QuizItem; chosenIndex: number } => h.chosenIndex != null);

  const progress = (n: number, total: number) => (
    <div className="h-2 overflow-hidden rounded-full" style={{ background: c.surfaceVariant }}>
      <div className="h-full rounded-full transition-all duration-300" style={{ width: `${((n + 1) / total) * 100}%`, background: NAV_COLORS.exam }} />
    </div>
  );

  return (
    <div className="flex flex-col gap-3">
      <ScreenHeader c={c} title="آزمون" subtitle="آزمون تستی و تمرین تشریحی — از ده‌ها آزمون واقعی کنگره ۶۰" accent={NAV_COLORS.exam} />

      {phase === "start" && nightBanner && (
        <div className="rounded-2xl p-3.5" style={{ background: "#7C3AED14", border: "1px solid #7C3AED66" }}>
          <p className="text-[12.5px] font-black" style={{ color: "#6D28D9" }}>
            🌙 آزمون شبانه — امشب هنوز آزمون ندادی!
          </p>
          <p className="mt-1 text-[11px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
            چند دقیقه وقت بگذار و آزمون شب را بده؛ سؤال‌های امشب با دفعهٔ قبل فرق دارند. ما می‌توانیم ✋
          </p>
          <div className="mt-2 flex gap-2">
            <button
              onClick={() => {
                setNightBanner(false);
                setMode("quiz");
                startQuiz({ len: 5 });
              }}
              className="flex-1 rounded-xl py-2.5 text-[12.5px] font-bold active:scale-[0.98]"
              style={{ background: "#7C3AED", color: "#FFFFFF" }}
            >
              شروع آزمون شبانه (۵ سؤال)
            </button>
            <button
              onClick={() => setNightBanner(false)}
              className="rounded-xl border px-4 py-2.5 text-[12.5px] font-bold"
              style={{ borderColor: c.outline, color: c.onBg, background: c.surface }}
            >
              بستن
            </button>
          </div>
        </div>
      )}

      {phase === "start" && (
        <>
          {/* پنج کارت اصلی صفحهٔ شروع — طرح مصوب «عکس خوب» + حفظ امکانات فعلی */}
          {EXAM_CARDS.map((cd) => {
            const on = card === cd.id;
            const Icon = cd.icon;
            return (
              <div key={cd.id} className="flex flex-col gap-2">
                <button
                  onClick={() => {
                    if (on) {
                      setCard(null);
                      return;
                    }
                    setCard(cd.id);
                    setReviewEmpty(false);
                    if (cd.id === "written") setMode("flash");
                    else setMode("quiz");
                    if (cd.id === "term") setQuizLen(100);
                    if (cd.id === "cat" && quizLen > 50) setQuizLen(20);
                  }}
                  aria-expanded={on}
                  className="flex w-full items-center gap-3 rounded-2xl border px-3 py-3.5 text-right active:scale-[0.99]"
                  style={{
                    background: on ? `${cd.color}0d` : c.surface,
                    borderColor: on ? cd.color : c.outlineVariant,
                    borderWidth: on ? 1.75 : 1.25,
                    boxShadow: on ? `0 4px 14px -6px ${cd.color}55` : "none",
                  }}
                >
                  <span className="min-w-0 flex-1">
                    <span className="block text-[14.5px] font-black" style={{ color: on ? cd.color : c.onBg }}>
                      {cd.title}
                    </span>
                    <span className="mt-0.5 block text-[11px] leading-5" style={{ color: c.onSurfaceVariant }}>
                      {cd.sub}
                    </span>
                  </span>
                  <span className="relative grid h-11 w-11 shrink-0 place-items-center rounded-full" style={{ background: `${cd.color}1f` }}>
                    <Icon size={21} color={cd.color} strokeWidth={2.1} />
                    {cd.id === "review" && wrongCount > 0 && (
                      <span
                        className="absolute -left-1 -top-1 grid h-[18px] min-w-[18px] place-items-center rounded-full px-1 text-[9.5px] font-black text-white"
                        style={{ background: cd.color }}
                      >
                        {faNum(wrongCount)}
                      </span>
                    )}
                  </span>
                </button>

                {on && cd.id === "term" && (
                  <SectionCard c={c} title="تنظیم آزمون ترم" accent={cd.color}>
                    <p className="text-[11.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                      همهٔ منابع قاطی — مثل جلسهٔ ترم؛ ۱۰۰ سؤال استاندارد یا هر تعدادی که انتخاب کنی.
                    </p>
                    <p className="mt-1 text-center text-[12.5px] font-bold" style={{ color: c.onBg }}>
                      تعداد سؤال:
                    </p>
                    <div className="flex flex-wrap justify-center gap-1.5">
                      {QUIZ_LENS.map((n) => (
                        <button
                          key={n}
                          onClick={() => setQuizLen(n)}
                          className="rounded-full border px-3 py-1.5 text-[12.5px] font-bold active:scale-[0.98]"
                          style={{
                            background: quizLen === n ? `${cd.color}14` : c.surface,
                            borderColor: quizLen === n ? cd.color : c.outlineVariant,
                            color: quizLen === n ? cd.color : c.onSurfaceVariant,
                          }}
                        >
                          {faNum(n)}
                        </button>
                      ))}
                    </div>
                    <div className="flex justify-center pt-1">
                      <button
                        onClick={() => startQuiz({ len: quizLen, cats: new Set() })}
                        disabled={!bank}
                        className="rounded-xl px-6 py-2.5 text-[13.5px] font-bold active:scale-[0.98] disabled:opacity-50"
                        style={{ background: cd.color, color: "#FFFFFF" }}
                      >
                        شروع آزمون ترم
                      </button>
                    </div>
                  </SectionCard>
                )}

                {on && cd.id === "cat" && (
                  <SectionCard c={c} title="انتخاب منبع" accent={cd.color}>
                    <div className="flex flex-wrap justify-center gap-2">
                      {quizCatOptions.map((cat) => {
                        const sel = quizCats.has(cat);
                        return (
                          <button
                            key={cat}
                            onClick={() =>
                              setQuizCats(sel ? new Set([...quizCats].filter((x) => x !== cat)) : new Set([...quizCats, cat]))
                            }
                            className="w-[31%] rounded-2xl border px-1 py-3.5 text-center text-[11.5px] font-bold leading-5 active:scale-[0.98]"
                            style={{
                              background: sel ? `${cd.color}14` : c.surface,
                              borderColor: sel ? cd.color : c.outlineVariant,
                              color: sel ? cd.color : c.onSurfaceVariant,
                            }}
                          >
                            {cat}
                          </button>
                        );
                      })}
                    </div>
                    <p className="text-center text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
                      هیچ دسته‌ای انتخاب نشود = همهٔ منابع
                    </p>
                    {/* منابع جزوه‌ای (کنگره ۶۰ چیست، قوانین، حرمت، سفر اول و دوم، بهترین راه، مصرف‌کننده کیست) اکنون در چیپ‌های بالا فعال‌اند */}
                    <p className="mt-1 text-center text-[12.5px] font-bold" style={{ color: c.onBg }}>
                      تعداد سؤال:
                    </p>
                    <div className="flex flex-wrap justify-center gap-1.5">
                      {QUIZ_LENS.map((n) => (
                        <button
                          key={n}
                          onClick={() => setQuizLen(n)}
                          className="rounded-full border px-3 py-1.5 text-[12.5px] font-bold active:scale-[0.98]"
                          style={{
                            background: quizLen === n ? `${cd.color}14` : c.surface,
                            borderColor: quizLen === n ? cd.color : c.outlineVariant,
                            color: quizLen === n ? cd.color : c.onSurfaceVariant,
                          }}
                        >
                          {faNum(n)}
                        </button>
                      ))}
                    </div>
                    <div className="flex justify-center pt-1">
                      <button
                        onClick={() => startQuiz({ len: quizLen, cats: quizCats })}
                        disabled={!bank}
                        className="rounded-xl px-6 py-2.5 text-[13.5px] font-bold active:scale-[0.98] disabled:opacity-50"
                        style={{ background: cd.color, color: "#FFFFFF" }}
                      >
                        شروع تمرین دسته‌ای
                      </button>
                    </div>
                  </SectionCard>
                )}

                {on && cd.id === "quick" && (
                  <SectionCard c={c} accent={cd.color}>
                    <p className="text-[11.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                      ۵ سؤال از همهٔ منابع، بدون هیچ تنظیمی — برای وقتی که فقط چند دقیقه وقت داری.
                    </p>
                    <div className="flex justify-center pt-1">
                      <button
                        onClick={() => startQuiz({ len: 5, cats: new Set() })}
                        disabled={!bank}
                        className="rounded-xl px-6 py-2.5 text-[13.5px] font-bold active:scale-[0.98] disabled:opacity-50"
                        style={{ background: cd.color, color: "#FFFFFF" }}
                      >
                        شروع آزمون ۵ سؤالی
                      </button>
                    </div>
                  </SectionCard>
                )}

                {on && cd.id === "review" && (
                  <SectionCard c={c} accent={cd.color}>
                    {wrongCount > 0 ? (
                      <>
                        <p className="text-[11.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                          {faNum(wrongCount)} سؤال غلط ذخیره شده است؛ هر سؤالی که این‌جا درست جواب بدهی از فهرست مرور حذف می‌شود.
                        </p>
                        <div className="flex justify-center pt-1">
                          <button
                            onClick={() => startQuiz({ review: true })}
                            className="rounded-xl px-6 py-2.5 text-[13.5px] font-bold active:scale-[0.98]"
                            style={{ background: cd.color, color: "#FFFFFF" }}
                          >
                            شروع مرور اشتباه‌ها
                          </button>
                        </div>
                      </>
                    ) : (
                      <p className="text-[11.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                        هنوز سؤال غلطی ذخیره نشده — اول یک آزمون بده؛ هر سؤالی که غلط جواب بدهی خودکار این‌جا ذخیره می‌شود تا دفعهٔ بعد بیشتر تکرارش کنی.
                      </p>
                    )}
                    {reviewEmpty && (
                      <p className="text-center text-[11px] font-bold" style={{ color: RED_NO }}>
                        هنوز غلطی ذخیره نشده است.
                      </p>
                    )}
                  </SectionCard>
                )}

                {on && cd.id === "written" && (
                  <SectionCard c={c} title="تمرین تشریحی" accent={cd.color}>
                    <p className="text-[12px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
                      {bank
                        ? `${faNum(flashShortOnly ? shortQaCount : bankQaUsable.length)} سؤال تشریحی پاسخ‌دار ${flashShortOnly ? "متن کوتاه" : ""} از ۱۴ وادی عشق، ۱۴ مقاله، راهنما و راهنمایی و… اول خودت پاسخ بده، بعد پاسخ را ببین و صادقانه علامت بزن.`
                        : "در حال بارگذاری بانک سؤال…"}
                    </p>
                    {/* حالت طول پاسخ: کوتاه (پیش‌فرض) یا همهٔ سؤال‌ها */}
                    <div className="flex flex-wrap justify-center gap-1.5">
                      {([
                        [true, "متن کوتاه"],
                        [false, "همهٔ سؤال‌ها"],
                      ] as const).map(([val, label]) => {
                        const sel = flashShortOnly === val;
                        return (
                          <button
                            key={label}
                            onClick={() => setFlashShortOnly(val)}
                            className="rounded-full border px-3 py-1.5 text-[11.5px] font-bold active:scale-[0.98]"
                            style={{
                              background: sel ? `${cd.color}14` : c.surface,
                              borderColor: sel ? cd.color : c.outlineVariant,
                              color: sel ? cd.color : c.onSurfaceVariant,
                            }}
                          >
                            {label}
                          </button>
                        );
                      })}
                    </div>
                    <p className="text-center text-[10.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                      در حالت «متن کوتاه» فقط پاسخ‌های کوتاه (تا ۱۲۰ حرف) می‌آید — مناسب مرور سریع؛ پاسخ‌های بلند با گزینهٔ «همهٔ سؤال‌ها» تمرین می‌شوند. سؤال‌های «۶۰ درجه» و «تصاویر کتاب ۶۰ درجه» در هر دو حالت می‌آیند.
                    </p>
                    <div className="flex flex-wrap justify-center gap-2">
                      {flashCatOptions.map((cat) => {
                        const sel = flashCats.has(cat);
                        return (
                          <button
                            key={cat}
                            onClick={() =>
                              setFlashCats(sel ? new Set([...flashCats].filter((x) => x !== cat)) : new Set([...flashCats, cat]))
                            }
                            className="w-[31%] rounded-2xl border px-1 py-3.5 text-center text-[11.5px] font-bold leading-5 active:scale-[0.98]"
                            style={{
                              background: sel ? `${cd.color}14` : c.surface,
                              borderColor: sel ? cd.color : c.outlineVariant,
                              color: sel ? cd.color : c.onSurfaceVariant,
                            }}
                          >
                            {cat}
                          </button>
                        );
                      })}
                    </div>
                    <p className="text-center text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
                      هیچ دسته‌ای انتخاب نشود = همهٔ منابع
                    </p>
                    <div className="flex justify-center pt-1">
                      <button
                        onClick={startFlash}
                        disabled={!bank}
                        className="rounded-xl px-6 py-2.5 text-[13.5px] font-bold active:scale-[0.98] disabled:opacity-50"
                        style={{ background: cd.color, color: "#FFFFFF" }}
                      >
                        شروع تمرین
                      </button>
                    </div>
                  </SectionCard>
                )}
              </div>
            );
          })}

          {/* کارت آزمون شبانه */}
          <SectionCard c={c} title="آزمون شبانه 🌙" accent="#7C3AED">
            <p className="text-[11.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
              هر شب در ساعت انتخابی، اگر آن روز هنوز آزمون یا تمرینی نداده باشی، یادآوری می‌فرستد: «آزمون شب را هنوز ندادی!». با اولین آزمون یا تمرین همان روز، دیگر پیامی نمی‌آید.
              (در نسخهٔ اندروید، یادآوری واقعی نوتیفیکیشن می‌شود — حتی وقتی برنامه بسته است.)
            </p>
            <div className="mt-2 flex items-center gap-3">
              <div className="flex-1">
                <p className="text-[12.5px] font-bold" style={{ color: night.enabled ? "#6D28D9" : c.onSurfaceVariant }}>
                  {night.enabled ? "یادآوری فعال است" : "یادآوری خاموش است"}
                </p>
                <p className="mt-0.5 text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
                  {night.enabled
                    ? `روزانه ساعت ${faNum(night.hour)}:${night.minute === 0 ? "۰۰" : "۳۰"}${nightDone ? " — امروز آزمون دادی ✅" : " — امروز هنوز آزمون ندادی"}`
                    : "با فعال‌کردن، ساعت یادآوری را انتخاب کن"}
                </p>
              </div>
              <button
                role="switch"
                aria-checked={night.enabled}
                aria-label="فعال‌سازی یادآوری آزمون شبانه"
                onClick={() => setNightExam({ enabled: !night.enabled })}
                className="relative h-7 w-12 rounded-full transition-colors"
                style={{ background: night.enabled ? "#7C3AED" : c.surfaceVariant }}
              >
                <span
                  className="absolute top-1 h-5 w-5 rounded-full bg-white shadow transition-all"
                  style={{ right: night.enabled ? 4 : 28 }}
                />
              </button>
            </div>
            {night.enabled && (
              <div className="mt-2 flex flex-wrap justify-center gap-1.5">
                {[
                  [21, 0],
                  [21, 30],
                  [22, 0],
                  [22, 30],
                  [23, 0],
                ].map(([h, m]) => {
                  const sel = night.hour === h && night.minute === m;
                  return (
                    <button
                      key={`${h}-${m}`}
                      onClick={() => setNightExam({ enabled: true, hour: h, minute: m })}
                      className="rounded-full border px-3.5 py-1.5 text-[12px] font-bold active:scale-[0.98]"
                      style={{
                        background: sel ? "#7C3AED14" : c.surface,
                        borderColor: sel ? "#7C3AED" : c.outlineVariant,
                        color: sel ? "#6D28D9" : c.onSurfaceVariant,
                      }}
                    >
                      {faNum(h)}:{m === 0 ? "۰۰" : "۳۰"}
                    </button>
                  );
                })}
              </div>
            )}
          </SectionCard>

          <p className="text-center text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
            {bank
              ? `${faNum(bankMcqUsable.length)} سؤال تستی کلیددار + ${faNum(CORE_DST.length)} سؤال متد DST و ${faNum(bankQaUsable.length)} سؤال تشریحی پاسخ‌دار`
              : "در حال بارگذاری بانک سؤال…"}
          </p>

          {noKeyMsg && (
            <SectionCard c={c}>
              <p className="text-[11.5px] leading-5" style={{ color: RED_NO, textAlign: "justify" }}>
                برای دسته‌های انتخابی هنوز کلید پاسخ‌ها در بانک ثبت نشده است؛ فعلاً دسته‌های دیگر را انتخاب کن یا هیچ دسته‌ای را انتخاب نکن تا از همهٔ منابع کلیددار سؤال بیاید.
              </p>
            </SectionCard>
          )}

          {/* کارت توضیح پایین — متن مصوب */}
          <div className="rounded-2xl p-3.5" style={{ background: "#FCE7F3", border: "1px solid #F9A8D4" }}>
            <p className="text-[11.5px] leading-6" style={{ color: "#9D174D", textAlign: "justify" }}>
              هوشمندسازی آزمون: هر بار که وارد آزمون شوی، سؤال‌ها تصادفی چیده می‌شوند و تا جای ممکن از سؤال‌های تازه انتخاب می‌شوند — سؤال‌های سشن قبل، آخرِ صف می‌آیند تا تکرار نشوند. زیر هر سؤال برچسب منبع می‌آید (مثلاً «قوانین کنگره ۶۰») و چیزی که غلط جواب بدی، دفعهٔ بعد بیشتر تکرار می‌شود.
            </p>
          </div>

          {bankError && (
            <SectionCard c={c}>
              <p className="text-[11.5px] leading-5" style={{ color: RED_NO, textAlign: "justify" }}>
                بارگذاری بانک سؤال ناموفق بود؛ صفحه را دوباره باز کن.
              </p>
            </SectionCard>
          )}
        </>
      )}

      {phase === "running" && mode === "quiz" && quizList[qIndex] && (
        <>
          <SectionCard c={c}>
            <div className="flex items-center gap-2">
              <button
                onClick={() => goQuiz(qIndex - 1)}
                disabled={qIndex === 0}
                aria-label="سؤال قبل"
                className="grid h-10 w-10 shrink-0 place-items-center rounded-full border shadow-md active:scale-95 disabled:opacity-30"
                style={{ borderColor: c.outlineVariant, background: c.surface, color: NAV_COLORS.exam }}
              >
                <ChevronRight size={18} />
              </button>
              <p className="flex-1 text-center text-[13px] font-bold" style={{ color: NAV_COLORS.exam }}>
                سؤال {faNum(qIndex + 1)} از {faNum(quizList.length)}
              </p>
              <button
                onClick={() => goQuiz(qIndex + 1)}
                disabled={quizPicks[qIndex + 1] == null}
                aria-label="سؤال بعدی"
                className="grid h-10 w-10 shrink-0 place-items-center rounded-full border shadow-md active:scale-95 disabled:opacity-30"
                style={{ borderColor: c.outlineVariant, background: c.surface, color: NAV_COLORS.exam }}
              >
                <ChevronLeft size={18} />
              </button>
            </div>
            {progress(qIndex, quizList.length)}
            <p className="text-center text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
              {quizList[qIndex].cat}
            </p>
          </SectionCard>

          <SectionCard c={c} title={quizList[qIndex].question}>
            {quizList[qIndex].options.map((option, i) => {
              const isCorrect = i === quizList[qIndex].correctIndex;
              const isChosen = chosen === i;
              const bg = answered && isCorrect ? `${GREEN_OK}24` : answered && isChosen && !isCorrect ? `${RED_NO}1f` : isChosen ? `${NAV_COLORS.exam}14` : c.surface;
              const border = answered && isCorrect ? GREEN_OK : answered && isChosen && !isCorrect ? RED_NO : isChosen ? NAV_COLORS.exam : c.outline;
              return (
                <button
                  key={i}
                  onClick={() => !answered && setChosen(i)}
                  className="flex w-full items-center gap-2.5 rounded-xl border px-3 py-2.5 text-right active:scale-[0.99]"
                  style={{ background: bg, borderColor: border }}
                >
                  <span
                    className="grid h-[22px] w-[22px] shrink-0 place-items-center rounded-full text-[10.5px] font-bold"
                    style={{ background: isChosen ? NAV_COLORS.exam : c.surfaceVariant, color: isChosen ? "#FFFFFF" : c.onSurfaceVariant }}
                  >
                    {faNum(i + 1)}
                  </span>
                  <span className="block text-[12.5px] leading-5" style={{ color: c.onBg, textAlign: "justify" }}>
                    {option}
                  </span>
                </button>
              );
            })}

            {answered ? (
              <>
                <p className="mt-1 text-[13px] font-bold" style={{ color: chosen === quizList[qIndex].correctIndex ? GREEN_OK : RED_NO }}>
                  {chosen === quizList[qIndex].correctIndex ? "✅ درست بود!" : `❌ پاسخ درست: ${quizList[qIndex].options[quizList[qIndex].correctIndex]}`}
                </p>
                <p className="text-[11px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
                  {quizList[qIndex].explanation}
                </p>
                <div className="flex justify-center pt-1">
                  <button
                    onClick={() => {
                      if (qIndex + 1 < quizList.length) goQuiz(qIndex + 1);
                      else {
                        recordWrongs();
                        markNightDone();
                        setPhase("done");
                      }
                    }}
                    className="rounded-xl px-5 py-2.5 text-[13px] font-bold active:scale-[0.98]"
                    style={{ background: NAV_COLORS.exam, color: "#FFFFFF" }}
                  >
                    {qIndex + 1 < quizList.length ? "سؤال بعدی" : "دیدن نتیجه"}
                  </button>
                </div>
              </>
            ) : (
              <div className="flex justify-center pt-1">
                <button
                  onClick={() => {
                    if (chosen != null) {
                      setAnswered(true);
                      setQuizPicks((prev) => {
                        const next = [...prev];
                        next[qIndex] = chosen;
                        return next;
                      });
                    }
                  }}
                  disabled={chosen == null}
                  className="rounded-xl px-5 py-2.5 text-[13px] font-bold active:scale-[0.98] disabled:opacity-50"
                  style={{ background: NAV_COLORS.exam, color: "#FFFFFF" }}
                >
                  ثبت پاسخ
                </button>
              </div>
            )}
          </SectionCard>

          <div className="flex items-center justify-between">
            <p className="text-[12.5px]" style={{ color: c.onSurfaceVariant }}>
              ✓ {faNum(quizScore)} · ✗ {faNum(quizWrong)}
            </p>
            <button
              onClick={() => {
                recordWrongs();
                setPhase("start");
                setCard(null);
              }}
              className="rounded-xl border px-4 py-2 text-[12.5px] font-bold active:scale-[0.99]"
              style={{ borderColor: c.outline, color: NAV_COLORS.exam, background: c.surface }}
            >
              پایان تمرین
            </button>
          </div>
        </>
      )}

      {phase === "running" && mode === "flash" && (
        <>
          {flashList[fIndex] == null ? (
            <SectionCard c={c}>
              <p className="text-[12.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
                {flashShortOnly
                  ? "در این دسته‌ها سؤالِ با پاسخ کوتاه پیدا نشد؛ گزینهٔ «همهٔ سؤال‌ها» را انتخاب کن یا دسته‌های دیگری را اضافه کن."
                  : "سؤالی در این دسته‌ها پیدا نشد؛ دسته‌های دیگری را انتخاب کن یا بانک را از منابع تازه به‌روز کن."}
              </p>
              <button
                onClick={() => setPhase("start")}
                className="w-fit rounded-xl border px-4 py-2 text-[12.5px] font-bold"
                style={{ borderColor: c.outline, color: NAV_COLORS.exam }}
              >
                بازگشت
              </button>
            </SectionCard>
          ) : (
            <>
              <SectionCard c={c}>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => goFlash(fIndex - 1)}
                    disabled={fIndex === 0}
                    aria-label="سؤال قبل"
                    className="grid h-10 w-10 shrink-0 place-items-center rounded-full border shadow-md active:scale-95 disabled:opacity-30"
                    style={{ borderColor: c.outlineVariant, background: c.surface, color: NAV_COLORS.exam }}
                  >
                    <ChevronRight size={18} />
                  </button>
                  <p className="flex-1 text-center text-[13px] font-bold" style={{ color: NAV_COLORS.exam }}>
                    سؤال {faNum(fIndex + 1)} از {faNum(flashList.length)}
                  </p>
                  <button
                    onClick={() => goFlash(fIndex + 1)}
                    disabled={flashScores[fIndex + 1] == null}
                    aria-label="سؤال بعدی"
                    className="grid h-10 w-10 shrink-0 place-items-center rounded-full border shadow-md active:scale-95 disabled:opacity-30"
                    style={{ borderColor: c.outlineVariant, background: c.surface, color: NAV_COLORS.exam }}
                  >
                    <ChevronLeft size={18} />
                  </button>
                </div>
                {progress(fIndex, flashList.length)}
                <p className="text-center text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
                  {flashList[fIndex].cat}
                </p>
              </SectionCard>

              <SectionCard c={c} title={flashList[fIndex].q}>
                {reveal ? (
                  <>
                    <div className="rounded-xl border p-3" style={{ background: `${GREEN_OK}14`, borderColor: `${GREEN_OK}80` }}>
                      <p className="text-[11.5px] font-bold" style={{ color: GREEN_OK }}>
                        پاسخ:
                      </p>
                      <p className="mt-1 text-[12.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
                        {flashList[fIndex].a}
                      </p>
                      {flashList[fIndex].ref && (
                        <p className="mt-1 text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
                          {flashList[fIndex].ref}
                        </p>
                      )}
                    </div>
                    <div className="flex justify-center gap-2 pt-1">
                      <button
                        onClick={() => {
                          setFlashScores((prev) => {
                            const next = [...prev];
                            next[fIndex] = true;
                            return next;
                          });
                          if (fIndex + 1 < flashList.length) goFlash(fIndex + 1);
                          else {
                            markNightDone();
                            setPhase("done");
                          }
                        }}
                        className="rounded-xl px-4 py-2.5 text-[12.5px] font-bold active:scale-[0.98]"
                        style={{ background: GREEN_OK, color: "#FFFFFF" }}
                      >
                        بلد بودم ✓
                      </button>
                      <button
                        onClick={() => {
                          setFlashScores((prev) => {
                            const next = [...prev];
                            next[fIndex] = false;
                            return next;
                          });
                          if (fIndex + 1 < flashList.length) goFlash(fIndex + 1);
                          else {
                            markNightDone();
                            setPhase("done");
                          }
                        }}
                        className="rounded-xl border px-4 py-2.5 text-[12.5px] font-bold active:scale-[0.98]"
                        style={{ borderColor: c.outline, color: c.onBg, background: c.surface }}
                      >
                        بلد نبودم ✗
                      </button>
                    </div>
                  </>
                ) : (
                  <button
                    onClick={() => setReveal(true)}
                    className="w-full rounded-xl py-3 text-[13.5px] font-bold active:scale-[0.99]"
                    style={{ background: NAV_COLORS.exam, color: "#FFFFFF" }}
                  >
                    نمایش پاسخ
                  </button>
                )}
              </SectionCard>

              <div className="flex items-center justify-between">
                <p className="text-[12.5px]" style={{ color: c.onSurfaceVariant }}>
                  ✓ {faNum(known)} · ✗ {faNum(unknown)}
                </p>
                <button
                  onClick={() => {
                    markNightDone();
                    setPhase("done");
                  }}
                  className="rounded-xl border px-4 py-2 text-[12.5px] font-bold"
                  style={{ borderColor: c.outline, color: NAV_COLORS.exam, background: c.surface }}
                >
                  پایان تمرین
                </button>
              </div>
            </>
          )}
        </>
      )}

      {phase === "done" && mode === "quiz" && (
        <>
          <SectionCard c={c} title="نتیجهٔ آزمون">
            <p className="text-[26px] font-black" style={{ color: NAV_COLORS.exam }}>
              {faNum(quizScore)} از {faNum(quizList.length)} درست
            </p>
            <div className="flex justify-center">
              <WeCanBadge c={c} />
            </div>
            {quizList.length > 0 && (
              <p className="text-[12.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
                {(() => {
                  const percent = Math.floor((quizScore * 100) / quizList.length);
                  if (percent >= 90) return "فوق‌العاده بود! آشنایی‌ات عالی است. ما می‌توانیم ✋";
                  if (percent >= 70) return "خیلی خوب بود! یک مرور کوتاه روی جدول پروتکل، تو را کامل می‌کند.";
                  if (percent >= 50) return "بد نبود؛ اما پیشنهاد می‌کنم صفحهٔ «پروتکل» را یک‌بار دیگر با دقت بخوانی.";
                  return "نگران نباش؛ هر مسافری اولش این‌جوری است. جزوه و صفحهٔ پروتکل را مرور کن و دوباره آزمون بده. ما می‌توانیم ✋";
                })()}
              </p>
            )}
            <div className="flex justify-center gap-2">
              <button onClick={startQuiz} className="rounded-xl px-4 py-2.5 text-[13px] font-bold active:scale-[0.98]" style={{ background: NAV_COLORS.exam, color: "#FFFFFF" }}>
                آزمون دوباره
              </button>
              <button
                onClick={() => setPhase("start")}
                className="rounded-xl border px-4 py-2.5 text-[13px] font-bold active:scale-[0.98]"
                style={{ borderColor: c.outline, color: NAV_COLORS.exam, background: c.surface }}
              >
                صفحهٔ اول
              </button>
            </div>
          </SectionCard>

          <SectionCard c={c} title="مرور پاسخ‌ها">
            {history.map((h, idx) => {
              const wasRight = h.chosenIndex === h.q.correctIndex;
              return (
                <div key={idx} className="border-b pb-2 last:border-0" style={{ borderColor: c.outlineVariant }}>
                  <p className="text-[12.5px] font-bold" style={{ color: wasRight ? GREEN_OK : RED_NO }}>
                    {faNum(idx + 1)}. {h.q.question}
                  </p>
                  <p className="mt-0.5 text-[11px]" style={{ color: c.onSurfaceVariant }}>
                    {wasRight
                      ? `پاسخ تو: ${h.q.options[h.chosenIndex]}`
                      : `پاسخ تو: ${h.q.options[h.chosenIndex]} — پاسخ درست: ${h.q.options[h.q.correctIndex]}`}
                  </p>
                </div>
              );
            })}
          </SectionCard>
        </>
      )}

      {phase === "done" && mode === "flash" && (
        <SectionCard c={c} title="خلاصهٔ تمرین">
          <p className="text-[20px] font-black" style={{ color: NAV_COLORS.exam }}>
            ✓ {faNum(known)} بلد بودم · ✗ {faNum(unknown)} بلد نبودم
          </p>
          <p className="text-[12.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
            سؤال‌های «بلد نبودم» را در جلسهٔ بعدی تمرین، اول مرور کن؛ تکرار، کلید یادگیری است. ما می‌توانیم ✋
          </p>
          <div className="flex justify-center gap-2">
            <button onClick={startFlash} className="rounded-xl px-4 py-2.5 text-[13px] font-bold active:scale-[0.98]" style={{ background: NAV_COLORS.exam, color: "#FFFFFF" }}>
              تمرین دوباره
            </button>
            <button
              onClick={() => setPhase("start")}
              className="rounded-xl border px-4 py-2.5 text-[13px] font-bold active:scale-[0.98]"
              style={{ borderColor: c.outline, color: NAV_COLORS.exam, background: c.surface }}
            >
              صفحهٔ اول
            </button>
          </div>
        </SectionCard>
      )}
    </div>
  );
}

/* ================================ محتوای زندهٔ کتابخانه ================================
   پیام امروز (مطالب تاثیرگذار چرخشی) — کارت «آرام‌سازی ذهن» به درخواست کاربر حذف شد
   بانک محتوا از public/content/featured.json خوانده می‌شود — آپدیت زنده از گیت‌هاب
   (مثل config.json «پلنر من»: بدون ساخت APK جدید، با بستن و باز کردن برنامه اعمال می‌شود) */
type FeatItem = { id: string; title: string; sub: string; text: string };

type ContentJson = { featured?: FeatItem[] };
const FEATURED_FALLBACK: FeatItem[] = (featuredJson as ContentJson).featured ?? [];

const FEAT_HIST_KEY = "hamrah_feat_hist";

/** تاریخچهٔ محتوای نمایش‌داده‌شده — تا تکرار نشود و هر بار محتوای تازه ببینیم */
const readHist = (key: string): string[] => {
  try {
    const v = JSON.parse(localStorage.getItem(key) ?? "[]") as unknown;
    return Array.isArray(v) ? (v as string[]).filter((x) => typeof x === "string") : [];
  } catch {
    return [];
  }
};
const saveHist = (key: string, id: string, max = 8) => {
  try {
    const h = [id, ...readHist(key).filter((x) => x !== id)].slice(0, max);
    localStorage.setItem(key, JSON.stringify(h));
  } catch {
    /* بی‌صدا */
  }
};
/** انتخاب بدون تکرار: از میان مواردی که اخیراً دیده نشده‌اند */
const pickRotating = <T extends { id: string }>(pool: T[], recentIds: string[]): T => {
  const fresh = pool.filter((x) => !recentIds.includes(x.id));
  const cand = fresh.length > 0 ? fresh : pool;
  return cand[Math.floor(Math.random() * cand.length)] ?? pool[0];
};

/* — بانک پاساژها: قطعه‌های خواندنی استخراج‌شده از کل کتاب‌های کنگره ۶۰ —
   ۱۴ وادی، ۱۴ مقاله، ۶۰ درجه، راهنما و راهنمایی، جهان‌بینی، راهنمای تازه‌واردین،
   پروتکل DST، اصلاحیه، مثلث‌های آموزشی + کتاب‌های اسکنی (OCR) —
   هر قطعه برچسب «منبع: نام کتاب» دارد؛ PDFهای امتحان طبق خواستهٔ کاربر وارد بانک نمی‌شوند */
type Passage = { id: string; src: string; n: number; text: string };

/* هر کارت کتاب از کدام منبع(ها) تغذیه می‌شود */
const LIB_SRC: Record<string, string[]> = {
  "lib-01": ["چهارده مقاله"],
  "lib-02": ["کنگره ۶۰ چیست؟"],
  "lib-03": ["راهنمای تازه‌واردین"],
  "lib-04": ["چهارده وادی عشق"],
  "lib-05": ["جهان‌بینی ۱", "جهان‌بینی ۲"],
  dst: ["پروتکل DST", "اصلاحیه پروتکل DST"],
  vazayef: ["شرح وظایف"],
  ghavanin: ["قوانین کنگره ۶۰"],
  shast: ["کتاب ۶۰ درجه"],
  behtarin: ["بهترین راه"],
  mosalas: ["مثلث‌های آموزشی"],
  masraf: ["مصرف‌کننده کیست؟"],
};

/* ================================ مثلث‌های آموزشی ================================
   فهرست کامل ۵۸ مثلث جزوهٔ رسمی «مثلث‌های آموزشی کنگره ۶۰» — هر مثلث با سه ضلع مجزا رسم می‌شود */
type TriangleDef = { n: number; title: string; sides: [string, string, string] };

const TRIANGLES: TriangleDef[] = [
  { n: 1, title: "مثلث کنگره ۶۰", sides: ["عدالت", "معرفت", "عمل سالم"] },
  { n: 2, title: "مثلث درمان اعتیاد", sides: ["جسم", "روان", "جهان‌بینی"] },
  { n: 3, title: "مثلث مهندسی اعتیاد", sides: ["تغذیه", "اشباع", "سازگاری"] },
  { n: 4, title: "مثلث دانایی", sides: ["تفکر", "تجربه", "آموزش"] },
  { n: 5, title: "مثلث تفکر", sides: ["مسیر حرکت", "مبدأ", "مقصد"] },
  { n: 6, title: "مثلث آموزش", sides: ["متون آموزشی", "شخص آموزش‌دهنده", "شخص آموزش‌گیرنده"] },
  { n: 7, title: "مثلث جهالت", sides: ["منیت", "ترس", "ناامیدی"] },
  { n: 8, title: "مثلث نفس", sides: ["امّاره", "لوّامه", "مطمئنه"] },
  { n: 9, title: "مثلث خوراک نفس", sides: ["تفکر", "صوت", "نور"] },
  { n: 10, title: "مثلث حیات (دنیا و آخرت)", sides: ["برزخ", "جهنم", "بهشت"] },
  { n: 11, title: "مثلث ساعت (در جهان فیزیکی)", sides: ["فضا", "زمان", "مکان"] },
  { n: 12, title: "مثلث آفرینش", sides: ["صوت", "نور", "حس"] },
  { n: 13, title: "مثلث پنهان‌کاری", sides: ["موضوع", "پنهان‌کننده", "کسی که از او پنهان می‌شود"] },
  { n: 14, title: "مثلث خودکشی", sides: ["سیستم ایکس (فیزیولوژی)", "علت", "شناخت"] },
  { n: 15, title: "مثلث قاضی (قضاوت)", sides: ["شاکی", "متشاکی", "دلایل و مدارک"] },
  { n: 16, title: "مثلث سیستم ایمنی", sides: ["تشخیص نیروهای دشمن", "تشخیص نیروهای خودی", "مشکوک (دشمن یا خودی؟)"] },
  { n: 17, title: "مثلث عمل", sides: ["توان فیزیکی (توانایی)", "مهارت و آموزش", "حس"] },
  { n: 18, title: "مثلث کلام", sides: ["راستین", "دروغین", "خنثی"] },
  { n: 19, title: "مثلث وادی سیزدهم", sides: ["تغییر", "تبدیل", "ترخیص"] },
  { n: 20, title: "مثلث انسان دوبعدی", sides: ["عشق", "عقل", "ایمان"] },
  { n: 21, title: "مثلث عشق", sides: ["سایه‌ها", "جاذبه", "حس"] },
  { n: 22, title: "مثلث تشخیص جایگاه", sides: ["خود فرد", "خانواده", "اجتماع"] },
  { n: 23, title: "مثلث جرقه", sides: ["تقدیر (گذشته)", "خواست (حال)", "فرمان الهی یا تفکرات (آینده)"] },
  { n: 24, title: "مثلث افزایش تکامل", sides: ["حرکت", "سرعت", "برکت"] },
  { n: 25, title: "مثلث نیرو", sides: ["القا", "احیا", "تحرک"] },
  { n: 26, title: "مثلث احیا", sides: ["طعام", "احیا شونده", "احیاکننده"] },
  { n: 27, title: "مثلث یاران", sides: ["سابقون", "راست (یمین)", "چپ (شمال)"] },
  { n: 28, title: "مثلث NGO موفق", sides: ["علم", "نیروی انسانی", "منابع مالی"] },
  { n: 29, title: "مثلث کشفیات جناب آقای مهندس", sides: ["OT و DST", "D.sap", "نیکوتین"] },
  { n: 30, title: "مثلث معما", sides: ["صورت‌مسئله", "حل‌کنندهٔ مسئله", "حل مسئله"] },
  { n: 31, title: "مثلث ادامهٔ حیات", sides: ["قدرت تفکر", "قدرت انجام عمل", "قدرت فیزیک بدن"] },
  { n: 32, title: "مثلث ازدواج", sides: ["مذکر", "مؤنث", "کار روی نقاط مشترک"] },
  { n: 33, title: "مثلث خوشبختی", sides: ["تجربهٔ دیروز", "استفادهٔ امروز", "امید به فردا"] },
  { n: 34, title: "مثلث تباهی", sides: ["حسرت دیروز", "اتلاف امروز", "ترس از فردا"] },
  { n: 35, title: "مثلث ذلت", sides: ["افسردگی", "خشم و خشونت", "ترس و اضطراب"] },
  { n: 36, title: "مثلث رحمت", sides: ["مسئولیت", "اخلاق", "واقعیت"] },
  { n: 37, title: "مثلث تخریب شخصیت", sides: ["احساس بی‌لیاقتی", "احساس گناه", "احساس تهدید"] },
  { n: 38, title: "مثلث جسم سالم و ایده‌آل", sides: ["ورزش", "تغذیه", "خواب"] },
  { n: 39, title: "مثلث پرسشگری", sides: ["چرا", "که چه بشود (که چی)", "چگونه"] },
  { n: 40, title: "مثلث نفس واحده", sides: ["روح", "جن", "انسان"] },
  { n: 41, title: "مثلث تعادل بیوشیمی مغز", sides: ["داروی مناسب", "زمان لازم", "روش مناسب"] },
  { n: 42, title: "مثلث گروه‌بندی معتادان", sides: ["نامزدهای اعتیاد", "دوستدار اعتیاد", "خواستار رهایی"] },
  { n: 43, title: "مثلث برخورد با سه گروه", sides: ["پیشگیری", "مهار", "درمان"] },
  { n: 44, title: "مثلث خواستاران رهایی", sides: ["بیکار", "شاغل", "آخر خطی"] },
  { n: 45, title: "مثلث صحبت", sides: ["کلام مناسب", "زمان مناسب", "مکان مناسب"] },
  { n: 46, title: "مثلث جان", sides: ["نفس", "روح", "جن"] },
  { n: 47, title: "مثلث مراتب دانایی", sides: ["علم‌الیقین", "عین‌الیقین", "حق‌الیقین"] },
  { n: 48, title: "مثلث لذت (جابجایی انرژی)", sides: ["از دست دادن انرژی", "به دست آوردن انرژی", "تغییر انرژی به صورتی دیگر"] },
  { n: 49, title: "مثلث معادله عملکرد", sides: ["حس (نیرو)", "مسیر حرکت", "عملکرد"] },
  { n: 50, title: "مثلث رهایی کامل", sides: ["ترک", "درمان", "تعادل"] },
  { n: 51, title: "مثلث روابط بین انسان‌ها", sides: ["خوب", "بد", "مجهول"] },
  { n: 52, title: "مثلث قیاس ما در موضوعات", sides: ["اطلاعات درست", "اطلاعات غلط", "اطلاعات بی‌ربط"] },
  { n: 53, title: "مثلث وظایف رهجو", sides: ["نسبت به راهنما", "نسبت به کنگره", "نسبت به خانواده و کار و زندگی"] },
  { n: 54, title: "مثلث ورزشکار موفق", sides: ["قدرت بدنی", "مهارت", "حس"] },
  { n: 55, title: "مثلث جسم", sides: ["نقشه جسم (ژن)", "تفکر و اندیشه (جهان‌بینی)", "نگهداری جسم"] },
  { n: 56, title: "مثلث روان", sides: ["جسم (فیزیولوژی)", "جهان‌بینی", "ضمیر ناخودآگاه"] },
  { n: 57, title: "مثلث تشکیل ساختار", sides: ["خواسته و حس", "بستر", "تفکر"] },
  { n: 58, title: "مثلث سه اصل رساندن غذا به شهر وجودی", sides: ["غذای مناسب", "میزان مناسب", "زمان مناسب"] },
];

/** رسم مثلث — هر ضلع روی رأس خودش (بالا، پایین‌راست، پایین‌چپ) نوشته می‌شود */
/* رنگ مثلث‌های آموزشی: مشکی (بنا به درخواست کاربر) */
const TRIANGLE_INK = "#111111";

function TriangleFigure({ t }: { t: TriangleDef }) {
  const color = TRIANGLE_INK;
  const chip: React.CSSProperties = {
    position: "absolute",
    maxWidth: 118,
    padding: "3px 9px",
    borderRadius: 999,
    background: `${color}1c`,
    color: color,
    fontSize: 10,
    fontWeight: 700,
    lineHeight: 1.55,
    textAlign: "center",
  };
  return (
    <div className="relative mx-auto" style={{ width: 244, height: 188 }}>
      <svg viewBox="0 0 244 188" className="absolute inset-0 h-full w-full" aria-hidden="true">
        <polygon points="122,50 222,160 22,160" fill={`${color}0d`} stroke={color} strokeWidth={2.2} strokeLinejoin="round" />
        <circle cx={122} cy={50} r={4.5} fill={color} />
        <circle cx={222} cy={160} r={4.5} fill={color} />
        <circle cx={22} cy={160} r={4.5} fill={color} />
      </svg>
      <span style={{ ...chip, top: 0, left: "50%", transform: "translateX(-50%)" }}>{t.sides[0]}</span>
      <span style={{ ...chip, bottom: 0, right: 0 }}>{t.sides[1]}</span>
      <span style={{ ...chip, bottom: 0, left: 0 }}>{t.sides[2]}</span>
    </div>
  );
}

/** بخش مثلث‌ها — مثلث واقعی رسم می‌شود، نه متن پشت سر هم */
function TrianglesSection({ c }: { c: ThemeColors }) {
  return (
    <div className="flex flex-col gap-2.5">
      <p className="text-[11px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
        هر مثلث سه ضلع دارد؛ اگر یک ضلع ضعیف شود، تعادل کل مثلث به هم می‌ریزد. هر ضلع روی رأس خودش نوشته شده است. (هر ۵۸ مثلث جزوهٔ «مثلث‌های آموزشی کنگره ۶۰»)
      </p>
      <div className="nice-scroll flex max-h-96 flex-col gap-3 overflow-y-auto pl-1">
        {TRIANGLES.map((t) => (
          <div key={t.n} className="rounded-2xl border p-3" style={{ borderColor: `${TRIANGLE_INK}55` }}>
            <p className="mb-1.5 text-center text-[12.5px] font-black" style={{ color: TRIANGLE_INK }}>
              {faNum(t.n)}. {t.title}
            </p>
            <TriangleFigure t={t} />
          </div>
        ))}
      </div>
    </div>
  );
}

/** پنجرهٔ «خوانش متن» — کاری که آیکون کتاب (📖) انجام می‌دهد */
function ReaderDialog(props: {
  c: ThemeColors;
  id: string;
  title: string;
  sub?: string;
  text: string;
  triangles?: boolean;
  color: string;
  playing: boolean;
  loading: boolean;
  onPlay: () => void;
  onClose: () => void;
}) {
  return (
    <div className="absolute inset-0 z-30 flex items-center justify-center bg-black/45 p-4" onClick={props.onClose}>
      <div
        className="nice-scroll max-h-[86%] w-full overflow-y-auto rounded-[22px] p-4 shadow-xl"
        style={{ background: props.c.surface }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="پنجرهٔ خوانش متن"
      >
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <p className="text-[15px] font-black" style={{ color: props.color }}>
              {props.title}
            </p>
            {props.sub && (
              <p className="mt-0.5 text-[10.5px]" style={{ color: props.c.onSurfaceVariant }}>
                {props.sub}
              </p>
            )}
          </div>
          <button
            onClick={props.onClose}
            aria-label="بستن پنجره"
            className="grid h-9 w-9 shrink-0 place-items-center rounded-full active:scale-95"
            style={{ background: props.c.surfaceVariant }}
          >
            <X size={17} color={props.c.onBg} />
          </button>
        </div>
        {props.triangles ? (
          <div className="mt-3">
            <TrianglesSection c={props.c} />
          </div>
        ) : (
          <p className="mt-3 text-[12.5px] leading-7" style={{ color: props.c.onBg, textAlign: "justify" }}>
            {props.text}
          </p>
        )}
        <button
          onClick={props.onPlay}
          className="mt-4 flex w-full items-center justify-center gap-2 rounded-full py-2.5 text-[13px] font-bold active:scale-[0.99]"
          style={{ background: props.color, color: "#FFFFFF" }}
        >
          {props.loading ? (
            <Loader2 size={16} className="animate-spin" />
          ) : props.playing ? (
            <Pause size={16} />
          ) : (
            <Play size={16} />
          )}
          {props.loading ? "در حال آماده‌سازی صوت…" : props.playing ? "توقف پخش" : "پخش خوانش متن"}
        </button>
      </div>
    </div>
  );
}

/** کارت کتابخانه — دقیقاً مطابق عکس ۱ و ۲: ▶ سمت چپ، 📖 سمت راست، فلشِ باز/بسته وسط */
function LibCardView(props: {
  c: ThemeColors;
  title: string;
  sub: string;
  text: string;
  color: string;
  open: boolean;
  playing: boolean;
  loading: boolean;
  playingPos: number;
  badge?: string;
  triangles?: boolean;
  onToggle: () => void;
  onPlay: () => void;
  onRead: () => void;
  trailing?: React.ReactNode;
}) {
  const { c } = props;
  return (
    <div className="rounded-2xl p-3.5 shadow-sm" style={{ background: c.surface }}>
      <div className="flex items-center gap-2.5">
        {/* 📖 خوانش متن — سمت راست (در RTL فرزند اول)؛ پنجرهٔ کامل متن را باز می‌کند */}
        <button
          onClick={props.onRead}
          aria-label="خوانش متن در پنجره"
          className="grid h-11 w-11 shrink-0 place-items-center rounded-full active:scale-95"
          style={{ background: `${props.color}1f` }}
        >
          <BookOpen size={18} color={props.color} />
        </button>

        <div className="min-w-0 flex-1 text-center">
          <p className="truncate text-[14px] font-black" style={{ color: c.onBg }}>
            {props.title}
          </p>
          <p className="mt-0.5 truncate text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
            {props.sub}
          </p>
          <p className="mt-1 text-right text-[11px] font-bold" style={{ color: c.onSurfaceVariant }}>متن + صوت</p>
          {props.badge && (
            <p className="mt-0.5 flex items-center justify-start gap-1 text-[10.5px] font-bold" style={{ color: props.color }}>
              <BookOpen size={11} />
              <span>{props.badge}</span>
            </p>
          )}
        </div>

        {/* ▶ پخش — سمت چپ مطابق عکس ۱ و ۲ */}
        <button
          onClick={props.onPlay}
          aria-label={props.playing ? "توقف پخش" : "پخش صوت"}
          className="grid h-11 w-11 shrink-0 place-items-center rounded-full active:scale-95"
          style={{ background: props.playing ? props.color : `${props.color}1f` }}
        >
          {props.loading ? (
            <Loader2 size={19} className="animate-spin" color={props.color} />
          ) : props.playing ? (
            <Pause size={19} color={props.color} />
          ) : (
            <Play size={19} color={props.color} />
          )}
        </button>
      </div>

      {props.playing && (
        <div className="mt-2.5 h-1.5 overflow-hidden rounded-full" style={{ background: c.surfaceVariant }}>
          <div className="h-full rounded-full" style={{ width: `${props.playingPos * 100}%`, background: props.color }} />
        </div>
      )}

      <button
        onClick={props.onToggle}
        className="mx-auto mt-1 flex w-fit items-center justify-center active:scale-95"
        style={{ color: props.color }}
        aria-expanded={props.open}
        aria-label={props.open ? "بستن متن" : "نمایش متن"}
      >
        <ChevronDown size={18} className={props.open ? "rotate-180 transition-transform" : "transition-transform"} />
      </button>

      {props.open &&
        (props.triangles ? (
          <div className="mt-2 rounded-xl p-3" style={{ background: c.bg }}>
            <TrianglesSection c={c} />
          </div>
        ) : (
          <p className="mt-2 rounded-xl p-3 text-[12px] leading-7" style={{ background: c.bg, color: c.onBg, textAlign: "justify" }}>
            {props.text}
          </p>
        ))}
      {props.trailing}
    </div>
  );
}

/* ================================ کتابخانهٔ صوتی ================================ */
function LibraryScreen({ c }: { c: ThemeColors }) {
  const accent = NAV_COLORS.lib;
  const [voice, setVoice] = useState<"m" | "f">("m");
  const [expanded, setExpanded] = useState<string | null>("feat");
  const [reader, setReader] = useState<{ id: string; title: string; sub?: string; text: string; triangles?: boolean; color: string } | null>(null);
  const [playingId, setPlayingId] = useState<string | null>(null);
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [playingPos, setPlayingPos] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  /* — بانک محتوای زنده: پیام امروز (۲۴ آموزه) + بانک پاساژهای کل کتاب‌ها — */
  const [featPool, setFeatPool] = useState<FeatItem[]>(FEATURED_FALLBACK);
  const [passPool, setPassPool] = useState<Passage[]>([]);
  const [feat, setFeat] = useState<FeatItem | null>(null);
  /* متن جاری هر کارت کتاب — با هر ورود از بانک همان کتاب چرخش می‌شود */
  const [cardTexts, setCardTexts] = useState<Record<string, string>>({});
  /* تیک چرخش: با هر ورود به تب و هر بازگشت به برنامه یکی اضافه می‌شود */
  const [tick, setTick] = useState(0);
  const lastPicks = useRef<Record<string, string[]>>({});

  /* آپدیت زنده: نسخهٔ تازهٔ بانک آموزه‌ها خوانده می‌شود */
  useEffect(() => {
    fetch("/content/featured.json", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : null))
      .then((d: ContentJson | null) => {
        if (d?.featured?.length) setFeatPool(d.featured);
      })
      .catch(() => {});
  }, []);

  /* بانک پاساژها: قطعه‌های خواندنی همهٔ کتاب‌ها (اندازهٔ بانک ≈ ۱٫۴MB — با کش مرورگر) */
  useEffect(() => {
    fetch("/content/passages.json?v=1")
      .then((r) => (r.ok ? r.json() : null))
      .then((d: { passages?: Passage[] } | null) => {
        if (d?.passages?.length) setPassPool(d.passages);
      })
      .catch(() => {});
  }, []);

  /* هوشمند: با بازگشت به برنامه (وقتی تب در پس‌زمینه بود) هم چرخش تازه انجام شود */
  useEffect(() => {
    const onVis = () => {
      if (document.visibilityState === "visible") setTick((t) => t + 1);
    };
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, []);

  /* — موتور صوت: همهٔ بخش‌ها صوت دارند (مثل بخش ۱۴ وادی) — */
  const stopTts = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.ontimeupdate = null;
      audioRef.current.onended = null;
    }
    setPlayingId(null);
    setPlayingPos(0);
  };

  const playTts = async (id: string, text: string, slow = false) => {
    if (playingId === id) {
      stopTts();
      return;
    }
    stopTts();
    setLoadingId(id);
    try {
      const res = await fetch("/api/tts", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text, voice, calm: slow }),
      });
      if (!res.ok) throw new Error("tts-failed");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      if (!audioRef.current) audioRef.current = new Audio();
      const a = audioRef.current;
      a.src = url;
      a.ontimeupdate = () => setPlayingPos(a.duration ? a.currentTime / a.duration : 0);
      a.onended = () => {
        setPlayingId(null);
        setPlayingPos(0);
      };
      await a.play();
      setPlayingId(id);
    } catch {
      setPlayingId(null);
    } finally {
      setLoadingId(null);
    }
  };

  /* چرخش هوشمند: با هر ورود به کتابخانه (باز شدن برنامه، جابجایی بین تب‌ها یا بازگشت به برنامه)
     ① پیام بالای کتابخانه از کل بانک (آموزه‌ها + قطعه‌های همهٔ کتاب‌ها با برچسب منبع)
     ② متن هر کارت کتاب از بانک همان کتاب
     به‌طور تصادفی و بدون ترتیب مشخص انتخاب می‌شود؛ تاریخچهٔ ۱۲تایی مانع تکرار است.
     دکمهٔ «پیام دیگر» حذف شد — چرخش کاملاً خودکار است */
  useEffect(() => {
    stopTts();
    const hist = readHist(FEAT_HIST_KEY);
    const passageFeat: FeatItem[] = passPool.map((p) => ({
      id: p.id,
      title: p.src,
      sub: `منبع: ${p.src} — قطعهٔ ${p.n}`,
      text: p.text,
    }));
    /* ~۴۰٪ از آموزه‌های کوتاه، بقیه از کل کتاب‌ها — همه با تاریخچهٔ ضدتکرار مشترک */
    const fromAmoozeh = featPool.length > 0 && Math.random() < 0.4;
    const pool: FeatItem[] = fromAmoozeh
      ? featPool
      : passageFeat.length > 0
        ? passageFeat
        : featPool;
    const f = pickRotating(pool, hist);
    saveHist(FEAT_HIST_KEY, f.id, 12);
    setFeat(f);
    /* — متن تازهٔ هر کارت کتاب از بانک پاساژهای همان کتاب — */
    const next: Record<string, string> = {};
    for (const item of LIB_ITEMS) {
      const srcs = LIB_SRC[item.id];
      const cand = srcs ? passPool.filter((p) => srcs.includes(p.src)) : [];
      if (cand.length === 0) {
        next[item.id] = item.text;
        continue;
      }
      const recent = lastPicks.current[item.id] ?? [];
      const fresh = cand.filter((p) => !recent.includes(p.id));
      const from = fresh.length > 0 ? fresh : cand;
      const pick = from[Math.floor(Math.random() * from.length)];
      lastPicks.current[item.id] = [pick.id, ...recent].slice(0, 3);
      next[item.id] = pick.text;
    }
    setCardTexts(next);
  }, [featPool, passPool, tick]);
  /* انتخاب گوینده — روی همهٔ صوت‌ها اعمال می‌شود و به‌خاطر سپرده می‌شود */
  useEffect(() => {
    try {
      const v = localStorage.getItem("hamrah_voice");
      if (v === "m" || v === "f") setVoice(v);
    } catch {
      /* بی‌صدا */
    }
    return () => {
      stopTts();
    };
  }, []);
  const pickVoice = (v: "m" | "f") => {
    if (v === voice) return;
    stopTts();
    setVoice(v);
    try {
      localStorage.setItem("hamrah_voice", v);
    } catch {
      /* بی‌صدا */
    }
  };

  return (
    <div className="flex flex-col gap-2.5">
      {/* سربرگ مطابق عکس ۱ */}
      <div className="mb-1">
        <h2 className="text-[24px] font-black" style={{ color: accent }}>
          کتابخانه
        </h2>
        <p className="mt-1 text-[12.5px] leading-6" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
          متن و خوانش کتاب‌ها و متون کنگرهٔ ۶۰ — با صدای مرد و زن
        </p>
      </div>

      {/* پیام امروز — محتوای تاثیرگذار چرخشی (کادر قرمز عکس ۱) */}
      {feat && (
        <LibCardView
          c={c}
          title={feat.title}
          sub={feat.sub}
          text={feat.text}
          color={accent}
          open={expanded === "feat"}
          playing={playingId === "feat"}
          loading={loadingId === "feat"}
          playingPos={playingPos}
          badge="صوت = خوانش همین متن"
          onToggle={() => setExpanded(expanded === "feat" ? null : "feat")}
          onPlay={() => playTts("feat", feat.text)}
          onRead={() => setReader({ id: "feat", title: feat.title, sub: feat.sub, text: feat.text, color: accent })}
        />
      )}

      <div className="flex gap-2">
        {(
          [
            ["m", "گویندهٔ مرد"],
            ["f", "گویندهٔ زن"],
          ] as const
        ).map(([v, label]) => (
          <button
            key={v}
            onClick={() => pickVoice(v)}
            className="flex flex-1 items-center justify-center gap-1.5 rounded-full border py-2.5 text-[13px] font-bold active:scale-[0.98]"
            style={{
              background: voice === v ? accent : c.surface,
              borderColor: voice === v ? accent : c.outlineVariant,
              color: voice === v ? "#FFFFFF" : c.onSurfaceVariant,
            }}
          >
            <Mic size={15} color={voice === v ? "#FFFFFF" : NAV_COLORS.journal} />
            {label}
          </button>
        ))}
      </div>

      {LIB_ITEMS.map((item) => {
        /* متن کارت = معرفی کامل و تمیز کتاب (همیشه خوانا و کامل) + قطعهٔ چرخشی از متن اصلی همان کتاب */
        const passage = cardTexts[item.id];
        const txt =
          passage && passage !== item.text
            ? `${item.text}\n\n— قطعه‌ای از متن اصلی «${item.title}»:\n${passage}`
            : item.text;
        return (
          <LibCardView
            key={item.id}
            c={c}
            title={item.title}
            sub={item.sub}
            text={txt}
            color={item.color}
            open={expanded === item.id}
            playing={playingId === item.id}
            loading={loadingId === item.id}
            playingPos={playingPos}
            badge="صوت = خوانش همین متن"
            triangles={item.id === "mosalas"}
            onToggle={() => setExpanded(expanded === item.id ? null : item.id)}
            onPlay={() => playTts(item.id, txt)}
            onRead={() =>
              setReader({ id: item.id, title: item.title, sub: item.sub, text: txt, triangles: item.id === "mosalas", color: item.color })
            }
          />
        );
      })}

      {/* پنجرهٔ خوانش متن — با کلیک روی آیکون کتاب (📖) هر کارت */}
      {reader && (
        <ReaderDialog
          c={c}
          id={reader.id}
          title={reader.title}
          sub={reader.sub}
          text={reader.text}
          triangles={reader.triangles}
          color={reader.color}
          playing={playingId === reader.id}
          loading={loadingId === reader.id}
          onPlay={() => playTts(reader.id, reader.text)}
          onClose={() => setReader(null)}
        />
      )}
    </div>
  );
}

/* ================================ تنظیمات ================================ */
function SettingsScreen(props: {
  c: ThemeColors;
  settings: UserSettings;
  setSettings: (s: UserSettings) => void;
  onResetAll: () => void;
}) {
  const { c, settings, setSettings, onResetAll } = props;
  const [showReset, setShowReset] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [pick, setPick] = useState({ jy: 1404, jm: 1, jd: 1 });
  const sub = SUBSTANCES.find((s) => s.id === settings.substance)!;

  const pickMaxDay = (jy: number, jm: number) => (jm <= 6 ? 31 : jm <= 11 ? 30 : jy % 4 === 3 ? 30 : 29);
  const openPicker = () => {
    const base = jalaliPartsOf(settings.startDate ?? Date.now());
    setPick(base);
    setShowDatePicker(true);
  };

  return (
    <div className="flex flex-col gap-3">
      <ScreenHeader c={c} title="تنظیمات درمان" subtitle="نوع ماده و برنامه درمانی را فقط با هماهنگی راهنمای خود انتخاب کن" accent={NAV_COLORS.settings} />

      <SectionCard c={c} accent={NAV_COLORS.settings}>
        <p className="text-[14.5px] font-bold" style={{ color: NAV_COLORS.settings }}>
          {settings.startDate != null ? `تاریخ شروع درمان: ${jalaliLong(settings.startDate)}` : "تاریخ شروع درمان: هنوز ثبت نشده است"}
        </p>
        <div className="flex items-center justify-center gap-2.5 pt-1">
          <button
            onClick={openPicker}
            className="rounded-full px-8 py-2.5 text-[13px] font-bold active:scale-[0.98]"
            style={{ background: NAV_COLORS.settings, color: "#FFFFFF" }}
          >
            انتخاب تاریخ
          </button>
          <button
            onClick={() => setSettings({ ...settings, startDate: normalizeDay(Date.now()) })}
            className="rounded-full px-8 py-2.5 text-[13px] font-bold active:scale-[0.98]"
            style={{ border: `1.25px solid ${c.outline}`, color: NAV_COLORS.settings, background: c.surface }}
          >
            از امروز
          </button>
        </div>
        <p className="text-[10.5px]" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
          تاریخ شروع، روز اول پله نخستِ برنامه ماده انتخابی است.
        </p>
      </SectionCard>

      <SectionCard c={c} title="نوع ماده مصرفی" accent={NAV_COLORS.settings}>
        {/* کاشی‌های شماره‌دار ۲تایی در ۴ ردیف — الگو: چیپ‌های «مواد مصرفی» (عکس ۱):
            نقطهٔ رنگی شماره‌دار + برچسب یک‌خطی، همهٔ کاشی‌ها هم‌اندازه */}
        <div className="grid grid-cols-2 gap-2">
          {SUBSTANCES.map((s, i) => {
            const on = settings.substance === s.id;
            const dot = SUBSTANCE_DOTS[s.id] ?? "#9E9E9E";
            return (
              <button
                key={s.id}
                onClick={() => setSettings({ ...settings, substance: s.id, consumption: defaultConsumption(s.id) })}
                className="flex items-center gap-2 rounded-2xl border px-2.5 py-3 text-right active:scale-[0.98]"
                style={{
                  background: on ? `${dot}14` : c.surface,
                  borderColor: on ? dot : c.outlineVariant,
                  borderWidth: on ? 1.75 : 1.25,
                  boxShadow: on ? `0 3px 10px -4px ${dot}66` : "none",
                }}
                aria-pressed={on}
              >
                <span
                  className="grid h-6 w-6 shrink-0 place-items-center rounded-full text-[10.5px] font-black text-white"
                  style={{ background: dot }}
                >
                  {faNum(i + 1)}
                </span>
                <span
                  className="whitespace-nowrap text-[12.5px] leading-none"
                  style={{ color: on ? dot : c.onBg, fontWeight: on ? 800 : 600 }}
                >
                  {s.label}
                </span>
              </button>
            );
          })}
        </div>
        {/* تأیید ثبت انتخاب — بلافاصله زیر همین لیست، در همان صفحه (طراحی مصوب) */}
        <div
          className="mt-3 flex items-center gap-2.5 rounded-xl p-2.5"
          style={{ background: "#16A34A14", border: "1.5px solid #16A34A55" }}
          role="status"
          aria-live="polite"
        >
          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full" style={{ background: "#16A34A" }}>
            <span className="text-[15px] font-bold text-white">✓</span>
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-[12.5px] font-bold" style={{ color: "#16A34A" }}>
              ثبت شد — «{sub.label}» انتخاب و ذخیره شد
            </p>
            <p className="text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
              برنامه اجرایی: {sub.program}
            </p>
          </div>
        </div>
      </SectionCard>

      {sub.consumptionLabel == null ? (
        <SectionCard c={c} title="مقدار مصرف">
          <p className="text-[12.5px] leading-6" style={{ color: c.onBg, textAlign: "justify" }}>
            {sub.consumptionHint}
          </p>
        </SectionCard>
      ) : (
        <SectionCard c={c} title={sub.consumptionLabel} accent={NAV_COLORS.settings}>
          <div className="flex flex-wrap justify-center gap-2">
            {(sub.options ?? []).map((option) => {
              const on = settings.consumption === option;
              return (
                <button
                  key={option}
                  onClick={() => setSettings({ ...settings, consumption: option })}
                  className="rounded-full border px-3 py-1.5 text-[12px] font-medium active:scale-[0.98]"
                  style={{
                    background: on ? `${NAV_COLORS.settings}1f` : c.surface,
                    borderColor: on ? NAV_COLORS.settings : c.outlineVariant,
                    color: on ? NAV_COLORS.settings : c.onSurfaceVariant,
                    fontWeight: on ? 700 : 500,
                  }}
                >
                  {sub.optionUnit ? sub.optionUnit(option) : faDose(option)}
                </button>
              );
            })}
          </div>
          <p className="text-[11.5px] leading-[18px] font-medium" style={{ color: NAV_COLORS.settings, textAlign: "justify" }}>
            {/* توضیح پویا — با هر کلیک روی اعداد، متن دقیقاً معنی همان عدد انتخابی را می‌گوید (رفع باگ ثابت‌ماندن متن) */}
            {sub.consumptionDetail ? sub.consumptionDetail(settings.consumption) : sub.consumptionHint}
          </p>
          <p className="text-[10.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
            قاعدهٔ کلی: {sub.consumptionHint}
          </p>
        </SectionCard>
      )}

      <SectionCard c={c} title="مدل ساعت وعده‌ها" accent={NAV_COLORS.settings}>
        <div className="flex flex-wrap justify-center gap-1.5">
          {(
            [
              [1, "مدل ۱ (۵–۱۳–۲۱)"],
              [2, "مدل ۲ (۶–۱۴–۲۲)"],
              [3, "مدل ۳ (۷–۱۵–۲۳)"],
            ] as const
          ).map(([model, label]) => {
            const on = settings.doseModel === model;
            return (
              <button
                key={model}
                onClick={() => setSettings({ ...settings, doseModel: model })}
                className="rounded-full border px-2.5 py-[7px] text-[11px] active:scale-[0.98]"
                style={{
                  background: on ? `${NAV_COLORS.settings}1f` : c.surface,
                  borderColor: on ? NAV_COLORS.settings : c.outlineVariant,
                  color: on ? NAV_COLORS.settings : c.onBg,
                  fontWeight: on ? 700 : 500,
                }}
              >
                {label}
              </button>
            );
          })}
        </div>
        <p className="text-[10.5px] leading-5" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
          چطور انتخاب کنم؟ سه وعده همیشه ۸ ساعت فاصله دارند؛ وعده اول بعد از بیداری و وعده آخر نزدیک خواب است. اگر حدود ۵ صبح بیدار می‌شوی مدل ۱، حدود ۶ صبح مدل ۲ و حدود ۷ صبح مدل ۳ مناسب است. ساعت وعده شب با تشخیص راهنما می‌تواند تا یک ساعت پایین‌تر بیاید.
        </p>
      </SectionCard>

      <NoticeCard
        c={c}
        text="همه پله‌ها و مقادیر این برنامه از «اصلاحیه پروتکل DST (۱۳۹۸/۳/۱)» و راهنمای ویرایش ۹۹ گرفته شده است. انتخاب پله شروع و هر تغییر دیگری فقط با صلاحدید راهنمای تو انجام می‌شود."
      />

      <SectionCard c={c} title="داده‌ها" accent={NAV_COLORS.settings}>
        <button
          onClick={() => setShowReset(true)}
          className="w-full rounded-xl border py-2.5 text-[13px] font-bold active:scale-[0.99]"
          style={{ borderColor: RED_NO, color: RED_NO, background: c.surface }}
        >
          پاک کردن همه داده‌ها
        </button>
        <p className="text-[10.5px]" style={{ color: c.onSurfaceVariant }}>
          همه اطلاعات فقط روی همین دستگاه ذخیره می‌شود و به هیچ سروری ارسال نمی‌گردد.
        </p>
      </SectionCard>

      <SectionCard c={c} title="درباره" accent={NAV_COLORS.settings}>
        <p className="text-[13px] font-bold" style={{ color: c.onBg }}>
          مسافر — همراه درمان اعتیاد
        </p>
        <p className="text-[11px] leading-6" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
          نسخه ۱٫۵٫۷ — اپلیکیشن آفلاین و متن‌باز
          <br />
          پروتکل بر اساس اصلاحیه DST (۱۳۹۸/۳/۱)، پروتکل ۱۳۸۸ و راهنمای ویرایش ۹۹ کنگره ۶۰
        </p>
        <p className="text-[13px] font-bold" style={{ color: NAV_COLORS.settings }}>
          ما می‌توانیم ✋
        </p>
      </SectionCard>

      {showDatePicker && (
        <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/40 p-4" onClick={() => setShowDatePicker(false)}>
          <div className="w-full rounded-[22px] p-4 shadow-xl" style={{ background: c.surface }} onClick={(e) => e.stopPropagation()}>
            <p className="text-[14.5px] font-bold" style={{ color: c.onBg }}>
              انتخاب تاریخ شروع درمان
            </p>
            <div className="mt-3 grid grid-cols-3 gap-2">
              <select
                value={pick.jd}
                onChange={(e) => setPick({ ...pick, jd: Number(e.target.value) })}
                className="rounded-xl border px-1 py-2 text-center text-[12.5px]"
                style={{ borderColor: c.outlineVariant, background: c.surface, color: c.onBg }}
                aria-label="روز"
              >
                {Array.from({ length: pickMaxDay(pick.jy, pick.jm) }, (_, i) => i + 1).map((d) => (
                  <option key={d} value={d}>
                    {faNum(d)}
                  </option>
                ))}
              </select>
              <select
                value={pick.jm}
                onChange={(e) => {
                  const jm = Number(e.target.value);
                  setPick({ ...pick, jm, jd: Math.min(pick.jd, pickMaxDay(pick.jy, jm)) });
                }}
                className="rounded-xl border px-1 py-2 text-center text-[12.5px]"
                style={{ borderColor: c.outlineVariant, background: c.surface, color: c.onBg }}
                aria-label="ماه"
              >
                {JALALI_MONTHS.map((m, i) => (
                  <option key={m} value={i + 1}>
                    {m}
                  </option>
                ))}
              </select>
              <select
                value={pick.jy}
                onChange={(e) => setPick({ ...pick, jy: Number(e.target.value) })}
                className="rounded-xl border px-1 py-2 text-center text-[12.5px]"
                style={{ borderColor: c.outlineVariant, background: c.surface, color: c.onBg }}
                aria-label="سال"
              >
                {Array.from({ length: 10 }, (_, i) => 1403 + i).map((y) => (
                  <option key={y} value={y}>
                    {faNum(y)}
                  </option>
                ))}
              </select>
            </div>
            <div className="mt-3 flex gap-2">
              <button
                onClick={() => {
                  setSettings({ ...settings, startDate: jalaliToMs(pick.jy, pick.jm, pick.jd) });
                  setShowDatePicker(false);
                }}
                className="flex-1 rounded-xl py-2.5 text-[13px] font-bold"
                style={{ background: c.primary, color: c.onPrimary }}
              >
                تأیید
              </button>
              <button
                onClick={() => setShowDatePicker(false)}
                className="flex-1 rounded-xl border py-2.5 text-[13px] font-bold"
                style={{ borderColor: c.outline, color: c.onBg }}
              >
                انصراف
              </button>
            </div>
          </div>
        </div>
      )}

      {showReset && (
        <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/40 p-4" onClick={() => setShowReset(false)}>
          <div className="w-full rounded-[22px] p-4 shadow-xl" style={{ background: c.surface }} onClick={(e) => e.stopPropagation()}>
            <p className="text-[14.5px] font-bold" style={{ color: c.onBg }}>
              پاک کردن همه داده‌ها؟
            </p>
            <p className="mt-2 text-[12px] leading-6" style={{ color: c.onSurfaceVariant, textAlign: "justify" }}>
              همه ثبت‌های روزانه و تنظیمات درمان حذف می‌شوند. این عمل قابل بازگشت نیست.
            </p>
            <div className="mt-3 flex gap-2">
              <button
                onClick={() => {
                  onResetAll();
                  setShowReset(false);
                }}
                className="flex-1 rounded-xl py-2.5 text-[13px] font-bold"
                style={{ background: RED_NO, color: "#FFFFFF" }}
              >
                بله، پاک کن
              </button>
              <button
                onClick={() => setShowReset(false)}
                className="flex-1 rounded-xl border py-2.5 text-[13px] font-bold"
                style={{ borderColor: c.outline, color: c.onBg }}
              >
                انصراف
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ================================ قاب و پوسته ================================ */
function useClock(): string | null {
  const [t, setT] = useState<string | null>(null);
  useEffect(() => {
    const tick = () =>
      setT(
        new Intl.DateTimeFormat("fa-IR", { hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date())
      );
    tick();
    const id = setInterval(tick, 20000);
    return () => clearInterval(id);
  }, []);
  return t;
}

export default function Page() {
  const [mounted, setMounted] = useState(false);
  const [tab, setTab] = useState<TabId>("home");
  const [themeId, setThemeId] = useState("NARENJI");
  const [themeMenuOpen, setThemeMenuOpen] = useState(false);
  const [settings, setSettings] = useState<UserSettings>({
    startDate: null,
    substance: "OPIUM_SMOKED",
    consumption: 0,
    doseModel: 2,
  });
  const [entries, setEntries] = useState<JournalEntry[]>([]);

  useEffect(() => {
    const t = setTimeout(() => {
      setMounted(true);
      let start = normalizeDay(Date.now());
      try {
        const th = localStorage.getItem("hamrah_theme");
        if (th && THEMES.some((x) => x.id === th)) setThemeId(th);
        const s = localStorage.getItem("hamrah_settings");
        if (s) {
          const parsed = JSON.parse(s) as UserSettings;
          if (parsed && parsed.substance) {
            setSettings(parsed);
            start = 0;
          }
        }
        const e = localStorage.getItem("hamrah_entries");
        if (e) setEntries(JSON.parse(e) as JournalEntry[]);
      } catch {
        /* تنظیم پیش‌فرض می‌ماند */
      }
      if (start !== 0) {
        setSettings((prev) => (prev.startDate == null ? { ...prev, startDate: start } : prev));
      }
    }, 0);
    return () => clearTimeout(t);
  }, []);

  const persistSettings = (s: UserSettings) => {
    setSettings(s);
    try {
      localStorage.setItem("hamrah_settings", JSON.stringify(s));
    } catch {
      /* بی‌صدا */
    }
  };
  const persistEntries = (list: JournalEntry[]) => {
    setEntries(list);
    try {
      localStorage.setItem("hamrah_entries", JSON.stringify(list));
    } catch {
      /* بی‌صدا */
    }
  };
  const pickTheme = (id: string) => {
    setThemeId(id);
    try {
      localStorage.setItem("hamrah_theme", id);
    } catch {
      /* بی‌صدا */
    }
    setThemeMenuOpen(false);
  };

  const theme = THEMES.find((t) => t.id === themeId) ?? THEMES[0];
  const c = theme.c;
  const clock = useClock();

  const go = (t: TabId) => {
    setTab(t);
    document.getElementById("app-scroll")?.scrollTo({ top: 0 });
  };

  return (
    <main
      className="flex min-h-[100dvh] w-full items-center justify-center sm:py-8"
      style={{ background: "#EFE9E1", direction: "rtl" }}
    >
      <div
        className="relative h-[100dvh] w-full overflow-hidden sm:h-[820px] sm:max-h-[94vh] sm:w-[392px] sm:rounded-[2.6rem] sm:p-[9px] sm:shadow-2xl"
        style={{ background: "linear-gradient(160deg,#3a332b,#171310)" }}
      >
        <div className="relative flex h-full w-full flex-col overflow-hidden sm:rounded-[2.05rem]" style={{ background: c.bg }}>
          {/* نوار وضعیت */}
          <div className="flex items-center justify-between px-5 pt-2.5 pb-1" style={{ background: c.topBar, color: c.topBarContent }}>
            <div className="flex items-center gap-1 text-[10px] opacity-80">
              <span>٪۸۴</span>
              <span className="inline-block h-[10px] w-5 rounded-[3px] border" style={{ borderColor: c.topBarContent }}>
                <span className="block h-full w-4/5 rounded-[2px]" style={{ background: "#22C55E" }} />
              </span>
            </div>
            <span className="text-[10.5px]" style={{ direction: "ltr" }}>
              {clock ?? "۱۴:۲۰"}
            </span>
          </div>

          {/* نوار بالا */}
          <div className="relative flex items-center justify-center py-2.5" style={{ background: c.topBar }}>
            <p className="text-[15.5px] font-black" style={{ color: c.topBarContent }}>
              همراه درمان اعتیاد
            </p>
            <div className="absolute left-3 top-1/2 -translate-y-1/2">
              <button
                onClick={() => setThemeMenuOpen(!themeMenuOpen)}
                aria-label="انتخاب تم"
                aria-expanded={themeMenuOpen}
                className="grid h-9 w-9 place-items-center rounded-full active:scale-95"
              >
                <Palette size={19} color={c.topBarContent} />
              </button>
              {themeMenuOpen && (
                <div className="absolute left-0 top-10 z-30 w-44 rounded-2xl p-1.5 shadow-xl" style={{ background: c.surface }}>
                  {THEMES.map((t) => (
                    <button
                      key={t.id}
                      onClick={() => pickTheme(t.id)}
                      className="flex w-full items-center gap-2 rounded-xl px-2.5 py-2 text-right active:scale-[0.99]"
                    >
                      <span className="h-4 w-4 rounded-full" style={{ background: t.swatch }} />
                      <span className="flex-1 text-[12.5px]" style={{ color: c.onBg, fontWeight: t.id === themeId ? 700 : 500 }}>
                        {t.labelFa}
                      </span>
                      {t.id === themeId && <Check size={15} color={c.primary} />}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* محتوا */}
          <div id="app-scroll" className="relative flex-1 overflow-y-auto overscroll-contain px-4 py-4">
            {!mounted ? (
              <div className="grid h-full place-items-center">
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent" style={{ borderColor: c.primary, borderTopColor: "transparent" }} />
              </div>
            ) : (
              <>
                {tab === "home" && <HomeScreen c={c} settings={settings} entries={entries} go={go} />}
                {tab === "plan" && <ProtocolScreen c={c} settings={settings} />}
                {tab === "journal" && <JournalScreen c={c} settings={settings} entries={entries} setEntries={persistEntries} />}
                {tab === "exam" && <ExamScreen c={c} />}
                {tab === "lib" && <LibraryScreen c={c} />}
                {tab === "settings" && (
                  <SettingsScreen
                    c={c}
                    settings={settings}
                    setSettings={persistSettings}
                    onResetAll={() => {
                      const fresh: UserSettings = { startDate: null, substance: "OPIUM_SMOKED", consumption: 0, doseModel: 2 };
                      persistSettings(fresh);
                      persistEntries([]);
                    }}
                  />
                )}
              </>
            )}
          </div>

          {/* نوار پایین — سبک «پلنر من» */}
          <nav className="flex items-stretch border-t px-1 pb-3 pt-1.5" style={{ background: c.surface, borderColor: c.outlineVariant }} aria-label="ناوبری اصلی">
            {TABS.map(({ id, label, color, Icon }) => {
              const on = tab === id;
              return (
                <button key={id} onClick={() => go(id)} className="flex flex-1 flex-col items-center gap-[2px] py-1" aria-label={label} aria-current={on ? "page" : undefined}>
                  <span className="grid h-[34px] w-[34px] place-items-center rounded-full" style={{ background: on ? `${color}29` : "transparent" }}>
                    <Icon size={on ? 23 : 21} color={color} strokeWidth={on ? 2.2 : 1.8} />
                  </span>
                  <span className="text-[9.5px]" style={{ color: on ? color : c.onSurfaceVariant, fontWeight: on ? 700 : 500 }}>
                    {label}
                  </span>
                </button>
              );
            })}
          </nav>
        </div>
      </div>
    </main>
  );
}
