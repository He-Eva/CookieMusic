/**
 * 表单校验规则
 *
 * 注册严格模式开关：
 */
export const USE_STRICT_SIGNUP = true;

// ---------- 登录 ----------
const validateName = (rule, value, callback) => {
  if (!value) {
    return callback(new Error("用户名不能为空"));
  }
  callback();
};

export const validatePassword = (rule, value, callback) => {
  if (value === "") {
    callback(new Error("密码不能为空"));
  } else {
    callback();
  }
};

export const SignInRules = {
  username: [{ validator: validateName, trigger: "blur", min: 3 }],
  password: [{ validator: validatePassword, trigger: "blur", min: 3 }],
};

// ---------- 注册：简单规则（测试默认） ----------
const SignUpRulesSimple = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur", min: 3 }],
  password: [{ required: true, message: "请输入密码", trigger: "blur", min: 3 }],
  sex: [{ required: true, message: "请选择性别", trigger: "change" }],
  phoneNum: [{ message: "手机号格式可选填", trigger: "blur" }],
  email: [
    { message: "请输入邮箱地址", trigger: "blur" },
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"],
    },
  ],
  birth: [{ required: true, type: "date", message: "请选择日期", trigger: "change" }],
  introduction: [{ message: "请输入介绍", trigger: "blur" }],
  location: [{ message: "请输入地区", trigger: "change" }],
};

// ---------- 注册：严格规则（正则，答辩演示） ----------
const USERNAME_RE = /^[a-zA-Z0-9_]{4,20}$/;
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d!@#$%^&*._-]{8,32}$/;
const PHONE_RE = /^1[3-9]\d{9}$/;

const validateUsernameRegex = (rule, value, callback) => {
  if (!value) return callback(new Error("用户名不能为空"));
  if (!USERNAME_RE.test(value)) {
    return callback(new Error("用户名为4-20位字母、数字或下划线"));
  }
  callback();
};

const validatePasswordRegex = (rule, value, callback) => {
  if (!value) return callback(new Error("密码不能为空"));
  if (!PASSWORD_RE.test(value)) {
    return callback(new Error("密码需8-32位且包含字母和数字"));
  }
  callback();
};

const validatePhoneRegex = (rule, value, callback) => {
  if (!value) return callback();
  if (!PHONE_RE.test(value)) return callback(new Error("手机号格式不正确"));
  callback();
};

const SignUpRulesStrict = {
  username: [{ validator: validateUsernameRegex, trigger: "blur" }],
  password: [{ validator: validatePasswordRegex, trigger: "blur" }],
  sex: [{ required: true, message: "请选择性别", trigger: "change" }],
  phoneNum: [{ validator: validatePhoneRegex, trigger: "blur" }],
  email: [
    { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] },
  ],
  birth: [{ required: true, type: "date", message: "请选择日期", trigger: "change" }],
  introduction: [{ max: 200, message: "签名不超过200字", trigger: "blur" }],
  location: [{ required: true, message: "请选择地区", trigger: "change" }],
};

/** 注册页 / 个人资料页共用（由开关切换） */
export const SignUpRules = USE_STRICT_SIGNUP ? SignUpRulesStrict : SignUpRulesSimple;
