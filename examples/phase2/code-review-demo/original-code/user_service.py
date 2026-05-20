"""
用户服务模块（包含多种待审查问题的原始代码）

此模块展示了常见的代码质量问题：
- 性能问题（N+1 查询）
- 安全问题（SQL 注入、硬编码密钥）
- 可读性问题（不清晰的命名、复杂的逻辑）
- 架构问题（职责混乱、耦合度高）
"""

import hashlib
import sqlite3
from datetime import datetime, timedelta
from typing import List, Dict, Optional


class UserService:
    """用户服务类 - 包含多种代码质量问题"""

    def __init__(self, db_path: str = "app.db"):
        self.db_path = db_path
        self.conn = sqlite3.connect(db_path)
        self.cursor = self.conn.cursor()
        self.admin_key = "sk-admin-12345"  # ⚠️ 问题 1: 硬编码密钥

    def get_users_for_dashboard(self, user_ids: List[int]) -> Dict:
        """
        获取用户信息用于仪表板展示
        
        ⚠️ 问题 2: N+1 查询问题（致命性能问题）
        """
        users = []
        for user_id in user_ids:  # 循环内查询 - 这将执行 N 次查询
            query = f"SELECT * FROM users WHERE id = {user_id}"  # ⚠️ 问题 3: SQL 注入风险
            self.cursor.execute(query)
            user = self.cursor.fetchone()
            users.append(user)

        result = {"users": users, "count": len(users), "timestamp": datetime.now()}
        return result

    def authenticate_user(self, username: str, password: str) -> Optional[Dict]:
        """
        用户认证
        
        ⚠️ 问题 4: 使用弱加密（MD5）、缺少登录尝试限制
        """
        # 使用不安全的 MD5，且没有盐值
        password_hash = hashlib.md5(password.encode()).hexdigest()

        query = f"SELECT * FROM users WHERE username = '{username}' AND password = '{password_hash}'"
        self.cursor.execute(query)
        user = self.cursor.fetchone()

        if user:
            return {"id": user[0], "username": user[1], "role": user[2]}
        return None

    def update_user_permissions(self, user_id: int, new_permissions: List[str]):
        """
        更新用户权限
        
        ⚠️ 问题 5: 缺少权限检查、权限提升漏洞
        """
        # 直接更新，没有检查当前用户是否有权进行此操作
        perm_str = ",".join(new_permissions)
        query = f"UPDATE users SET permissions = '{perm_str}' WHERE id = {user_id}"
        self.cursor.execute(query)
        self.conn.commit()

    def get_user_reports(self, user_id: int, start_date: str, end_date: str) -> List[Dict]:
        """
        获取用户报告
        
        ⚠️ 问题 6: 复杂度过高、嵌套过深、代码重复
        """
        reports = []
        
        # 第一步：获取用户
        q = f"SELECT * FROM users WHERE id = {user_id}"
        self.cursor.execute(q)
        u = self.cursor.fetchone()

        if u is None:
            return []

        # 第二步：获取报告（低效的逻辑）
        q = f"SELECT * FROM reports WHERE user_id = {user_id}"
        self.cursor.execute(q)
        rs = self.cursor.fetchall()

        # 第三步：过滤和处理（复杂的嵌套）
        for r in rs:
            r_date = datetime.strptime(r[2], "%Y-%m-%d")
            s_date = datetime.strptime(start_date, "%Y-%m-%d")
            e_date = datetime.strptime(end_date, "%Y-%m-%d")

            if s_date <= r_date <= e_date:
                # 第四步：获取报告的详细内容（又是 N+1 查询）
                q = f"SELECT * FROM report_details WHERE report_id = {r[0]}"
                self.cursor.execute(q)
                details = self.cursor.fetchall()

                r_dict = {
                    "id": r[0],
                    "title": r[1],
                    "date": r[2],
                    "details": details,
                }

                # 第五步：手工计算统计信息（低效）
                total = 0
                for d in details:
                    total = total + d[2]

                r_dict["total"] = total
                reports.append(r_dict)

        return reports

    def delete_old_sessions(self, days: int = 30):
        """
        删除旧的会话记录
        
        ⚠️ 问题 7: 没有事务保护、没有日志、没有错误处理
        """
        cutoff = datetime.now() - timedelta(days=days)
        query = f"DELETE FROM sessions WHERE created_at < '{cutoff}'"
        self.cursor.execute(query)
        self.conn.commit()
        print(f"删除了 {self.cursor.rowcount} 条旧会话")

    def export_user_data(self, user_id: int, format: str) -> str:
        """
        导出用户数据
        
        ⚠️ 问题 8: 路径遍历漏洞、缺少输入验证
        """
        # 允许任意路径，存在安全风险
        output_file = f"/exports/{format}/{user_id}_data.csv"

        query = f"SELECT * FROM users WHERE id = {user_id}"
        self.cursor.execute(query)
        user = self.cursor.fetchone()

        # 直接写入文件，没有验证路径
        with open(output_file, "w") as f:
            f.write(str(user))

        return output_file

    def search_users(self, query_str: str, limit: int = 100) -> List[Dict]:
        """
        搜索用户
        
        ⚠️ 问题 9: SQL 注入、没有查询结果限制、大量结果加载到内存
        """
        # 直接将用户输入拼接到 SQL，严重的 SQL 注入风险
        query = f"SELECT * FROM users WHERE (username LIKE '%{query_str}%' OR email LIKE '%{query_str}%')"
        self.cursor.execute(query)
        results = self.cursor.fetchall()  # 可能加载成千上万的记录

        users = []
        for r in results[:limit]:
            users.append({"id": r[0], "username": r[1], "email": r[2]})

        return users

    def batch_update_status(self, user_ids: List[int], new_status: str):
        """
        批量更新用户状态
        
        ⚠️ 问题 10: 循环内执行单个 UPDATE，而不是批量操作
        """
        for uid in user_ids:
            # 每个用户执行一次查询和更新 - 这将执行 2N 次操作
            self.cursor.execute(f"SELECT * FROM users WHERE id = {uid}")
            user = self.cursor.fetchone()

            if user:
                self.cursor.execute(
                    f"UPDATE users SET status = '{new_status}' WHERE id = {uid}"
                )
                self.conn.commit()

    def __del__(self):
        """清理资源"""
        if self.conn:
            self.conn.close()


# 使用示例
if __name__ == "__main__":
    service = UserService()

    # 这将执行 N 个查询（N = user_ids 的长度）
    result = service.get_users_for_dashboard([1, 2, 3, 4, 5])
    print(f"获取 {result['count']} 个用户")

    # 认证用户
    auth = service.authenticate_user("john", "password123")
    print(f"认证结果: {auth}")

    # 获取用户报告
    reports = service.get_user_reports(1, "2026-01-01", "2026-03-31")
    print(f"获取 {len(reports)} 个报告")

    # 删除旧会话
    service.delete_old_sessions(30)

    # 搜索用户
    search_results = service.search_users("john")
    print(f"搜索结果: {len(search_results)} 个用户")
