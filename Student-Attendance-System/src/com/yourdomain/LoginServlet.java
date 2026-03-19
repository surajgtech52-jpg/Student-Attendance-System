package com.yourdomain;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String teacherId = request.getParameter("teacherId");
        String password = request.getParameter("password");

        // IMPORTANT: Replace this with your actual database validation logic
        if ("admin".equals(teacherId) && "password".equals(password)) {
            HttpSession session = request.getSession();
            session.setAttribute("user", teacherId);
            response.sendRedirect("loginprocess.jsp");
        } else {
            response.sendRedirect("login_teacher.html?error=1");
        }
    }
}